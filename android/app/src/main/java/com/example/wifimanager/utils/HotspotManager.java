package com.example.wifimanager.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class HotspotManager {
    private static final String TAG = "HotspotManager";
    private static final String EXIT_CMD = "exit\n";
    private static final String BIN_SU = "su";
    private static final String BIN_IPTABLES = "iptables";
    private static final String BIN_TC = "tc";
    private final WifiManager wm;
    private static final List<String> BIN_PATHS = Arrays.asList("/system/bin/", "/system/xbin/", "/sbin/");
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    public interface OnHotspotStateListener {
        void onStarted(String ssid, String password);
        void onStopped();
        void onFailure(int reason);
    }

    public HotspotManager(Context c) {
        this.wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private String getBin(String name) {
        for (String path : BIN_PATHS) {
            try {
                File f = new File(path + name);
                if (f.exists()) return f.getAbsolutePath();
            } catch (Exception ignored) {}
        }
        return null;
    }

    public int setHotspotEnabled(boolean en, String s, String p) {
        try {
            if (!en) {
                stopLocalOnlyHotspot();
                toggleNatAsync(false);
            }

            if (tryRootMethod(en)) return 1;
            if (Build.VERSION.SDK_INT >= 26) return 2;

            return tryReflectionMethod(en, s, p);
        } catch (Exception e) {
            Log.e(TAG, "setHotspotEnabled critical error", e);
            return 0;
        }
    }

    private boolean tryRootMethod(boolean en) {
        String su = getBin(BIN_SU);
        if (su == null) return false;

        try {
            Process pr = new ProcessBuilder(su).start();
            try (DataOutputStream os = new DataOutputStream(pr.getOutputStream())) {
                String cmd = en ? "start-tethering" : "stop-tethering";
                os.writeBytes("cmd tethering " + cmd + " 0\n" + EXIT_CMD);
                os.flush();
            }
            if (en) toggleNatAsync(true);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Root method failed", e);
            return false;
        }
    }

    private int tryReflectionMethod(boolean en, String s, String p) {
        try {
            if (wm == null) return 0;
            if (en) wm.setWifiEnabled(false);
            WifiConfiguration conf = createWifiConfig(s, p);
            Method m = wm.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            Object res = m.invoke(wm, conf, en);
            if (en && Boolean.TRUE.equals(res)) {
                toggleNatAsync(true);
                return 4;
            }
            return Boolean.TRUE.equals(res) ? 1 : 0;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, "Reflection call failed", e);
            return 0;
        }
    }

    private WifiConfiguration createWifiConfig(String s, String p) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = s;
        if (p != null && p.length() >= 8) {
            conf.preSharedKey = p;
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        }
        return conf;
    }

    @SuppressLint("MissingPermission")
    public void startLocalOnlyHotspot(final OnHotspotStateListener listener) {
        if (Build.VERSION.SDK_INT >= 26 && wm != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            try {
                wm.startLocalOnlyHotspot(new LocalHotspotCallback(this, listener), handler);
            } catch (Exception e) {
                Log.e(TAG, "LocalHotspot start failed", e);
                if (listener != null) listener.onFailure(-2);
            }
        } else if (listener != null) {
            listener.onFailure(-1);
        }
    }

    public void stopLocalOnlyHotspot() {
        try {
            if (hotspotReservation != null) {
                hotspotReservation.close();
                hotspotReservation = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing hotspot", e);
        }
        toggleNatAsync(false);
    }

    public boolean isHotspotEnabled() {
        if (hotspotReservation != null) return true;
        try {
            if (wm == null) return false;
            Method m = wm.getClass().getDeclaredMethod("getWifiApState");
            Object res = m.invoke(wm);
            return res instanceof Integer && (Integer) res >= 12;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, "Error getting hotspot state", e);
            return false;
        }
    }

    public void toggleNatAsync(final boolean en) {
        new Thread(() -> {
            try {
                toggleNat(en);
            } catch (Exception e) {
                Log.e(TAG, "toggleNat thread error", e);
            }
        }).start();
    }

    private void toggleNat(boolean en) {
        String su = getBin(BIN_SU);
        String ipt = getBin(BIN_IPTABLES);
        if (su != null && ipt != null) {
            executeNatCommands(su, ipt, en);
        }
    }

    private void executeNatCommands(String su, String ipt, boolean en) {
        try {
            Process pr = new ProcessBuilder(su).start();
            try (DataOutputStream os = new DataOutputStream(pr.getOutputStream())) {
                if (en) {
                    os.writeBytes("echo 1 > /proc/sys/net/ipv4/ip_forward\n");
                    os.writeBytes(ipt + " -t nat -A POSTROUTING -j MASQUERADE\n");
                    os.writeBytes(ipt + " -A FORWARD -j ACCEPT\n");
                } else {
                    os.writeBytes(ipt + " -t nat -F\n");
                    os.writeBytes(ipt + " -F\n");
                }
                os.writeBytes(EXIT_CMD);
                os.flush();
            }
            pr.waitFor();
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "NAT commands failed", e);
            Thread.currentThread().interrupt();
        }
    }

    public void blockDevice(String mac, boolean b) {
        if (mac == null || !mac.contains(":")) return;
        String su = getBin(BIN_SU);
        String ipt = getBin(BIN_IPTABLES);
        if (su != null && ipt != null) {
            try {
                Process pr = new ProcessBuilder(su).start();
                try (DataOutputStream os = new DataOutputStream(pr.getOutputStream())) {
                    String op = b ? "I" : "D";
                    os.writeBytes(ipt + " -" + op + " FORWARD -m mac --mac-source " + mac + " -j DROP\n" + EXIT_CMD);
                    os.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "Block failed", e);
            }
        }
    }

    public void limitSpeed(String mac, int k) {
        if (mac == null) return;
        String su = getBin(BIN_SU);
        String tc = getBin(BIN_TC);
        if (su != null && tc != null) {
            try {
                Process pr = new ProcessBuilder(su).start();
                try (DataOutputStream os = new DataOutputStream(pr.getOutputStream())) {
                    os.writeBytes(tc + " qdisc add dev wlan0 root handle 1: htb default 10\n");
                    os.writeBytes(tc + " class add dev wlan0 parent 1: classid 1:1 htb rate " + k + "kbps ceil " + k + "kbps\n");
                    os.writeBytes(EXIT_CMD);
                    os.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "Limit failed", e);
            }
        }
    }
}

class LocalHotspotCallback extends WifiManager.LocalOnlyHotspotCallback {
    private final HotspotManager manager;
    private final HotspotManager.OnHotspotStateListener listener;

    LocalHotspotCallback(HotspotManager manager, HotspotManager.OnHotspotStateListener listener) {
        this.manager = manager;
        this.listener = listener;
    }

    @Override
    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
        manager.hotspotReservation = reservation;
        if (listener != null) {
            try {
                WifiConfiguration config = reservation.getWifiConfiguration();
                if (config != null) {
                    listener.onStarted(config.SSID, config.preSharedKey);
                } else {
                    listener.onStarted("Unknown", "");
                }
            } catch (Exception e) {
                listener.onStarted("Error", "");
            }
            manager.toggleNatAsync(true);
        }
    }

    @Override
    public void onStopped() {
        manager.hotspotReservation = null;
        if (listener != null) {
            try {
                listener.onStopped();
            } catch (Exception ignored) {}
        }
        manager.toggleNatAsync(false);
    }

    @Override
    public void onFailed(int reason) {
        if (listener != null) {
            try {
                listener.onFailure(reason);
            } catch (Exception ignored) {}
        }
    }
}
