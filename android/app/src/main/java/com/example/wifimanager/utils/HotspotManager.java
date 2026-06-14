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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class HotspotManager {
    private static final String TAG = "HotspotManager";
    private final Context ctx;
    private final WifiManager wm;
    private static final List<String> BIN_PATHS = Arrays.asList("/system/bin/", "/system/xbin/", "/sbin/");
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    public interface OnHotspotStateListener {
        void onStarted(String ssid, String password);
        void onStopped();
        void onFailure(int reason);
    }

    public HotspotManager(Context c) {
        this.ctx = c;
        this.wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private String getBin(String name) {
        for (String path : BIN_PATHS) {
            File f = new File(path + name);
            if (f.exists()) return f.getAbsolutePath();
        }
        return null;
    }

    public int setHotspotEnabled(boolean en, String s, String p) {
        Log.d(TAG, "setHotspotEnabled: " + en);
        if (!en) {
            stopLocalOnlyHotspot();
            toggleNatAsync(false);
        }

        if (tryRootMethod(en)) return 1;
        if (Build.VERSION.SDK_INT >= 26) return 2;

        return tryReflectionMethod(en, s, p);
    }

    private boolean tryRootMethod(boolean en) {
        String su = getBin("su");
        if (su == null) return false;

        try {
            Process pr = new ProcessBuilder(su).start();
            try (DataOutputStream os = new DataOutputStream(pr.getOutputStream())) {
                os.writeBytes("cmd tethering " + (en ? "start-tethering" : "stop-tethering") + " 0\nexit\n");
                os.flush();
            }
            if (en) toggleNatAsync(true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Root method failed", e);
            return false;
        }
    }

    private int tryReflectionMethod(boolean en, String s, String p) {
        try {
            if (en) wm.setWifiEnabled(false);
            WifiConfiguration conf = createWifiConfig(s, p);
            Method m = wm.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            Object res = m.invoke(wm, conf, en);
            if (en && res instanceof Boolean && (Boolean) res) {
                toggleNatAsync(true);
                return 4;
            }
            return (res instanceof Boolean && (Boolean) res) ? 1 : 0;
        } catch (Exception e) {
            Log.e(TAG, "Reflection method failed", e);
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
        if (Build.VERSION.SDK_INT >= 26) {
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
        if (hotspotReservation != null) {
            try {
                hotspotReservation.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing hotspot", e);
            }
            hotspotReservation = null;
        }
        toggleNatAsync(false);
    }

    public boolean isHotspotEnabled() {
        if (hotspotReservation != null) return true;
        try {
            Method m = wm.getClass().getDeclaredMethod("getWifiApState");
            Object res = m.invoke(wm);
            return res instanceof Integer && (Integer) res >= 12;
        } catch (Exception e) {
            Log.e(TAG, "Error getting hotspot state", e);
            return false;
        }
    }

    public void toggleNatAsync(final boolean en) {
        new Thread(() -> toggleNat(en)).start();
    }

    private void toggleNat(boolean en) {
        String su = getBin("su");
        String ipt = getBin("iptables");
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
                os.writeBytes("exit\n");
                os.flush();
            }
            pr.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "NAT commands failed", e);
        }
    }

    public void blockDevice(String mac, boolean b) {
        if (mac == null || !mac.contains(":")) return;
        String su = getBin("su");
        String ipt = getBin("iptables");
        if (su != null && ipt != null) {
            try {
                Process pr = new ProcessBuilder(su).start();
                try (DataOutputStream os = new DataOutputStream(pr.getOutputStream())) {
                    os.writeBytes(ipt + " -" + (b ? "I" : "D") + " FORWARD -m mac --mac-source " + mac + " -j DROP\nexit\n");
                    os.flush();
                }
            } catch (Exception e) {
                Log.e(TAG, "Block failed", e);
            }
        }
    }

    public void limitSpeed(String mac, int k) {
        if (mac == null) return;
        String su = getBin("su");
        String tc = getBin("tc");
        if (su != null && tc != null) {
            try {
                Process pr = new ProcessBuilder(su).start();
                try (DataOutputStream os = new DataOutputStream(pr.getOutputStream())) {
                    os.writeBytes(tc + " qdisc add dev wlan0 root handle 1: htb default 10\n");
                    os.writeBytes(tc + " class add dev wlan0 parent 1: classid 1:1 htb rate " + k + "kbps ceil " + k + "kbps\n");
                    os.writeBytes("exit\n");
                    os.flush();
                }
            } catch (Exception e) {
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
            WifiConfiguration config = reservation.getWifiConfiguration();
            if (config != null) {
                listener.onStarted(config.SSID, config.preSharedKey);
            } else {
                listener.onStarted("Unknown", "");
            }
            manager.toggleNatAsync(true);
        }
    }

    @Override
    public void onStopped() {
        manager.hotspotReservation = null;
        if (listener != null) listener.onStopped();
        manager.toggleNatAsync(false);
    }

    @Override
    public void onFailed(int reason) {
        if (listener != null) listener.onFailure(reason);
    }
}
