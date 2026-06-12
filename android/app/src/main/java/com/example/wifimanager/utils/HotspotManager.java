package com.example.wifimanager.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.DataOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class HotspotManager {
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
        if (!en) {
            stopLocalOnlyHotspot();
        }

        String su = getBin("su");
        if (su != null) {
            try {
                Process pr = new ProcessBuilder(su).start();
                DataOutputStream os = new DataOutputStream(pr.getOutputStream());
                os.writeBytes("cmd tethering " + (en ? "start-tethering" : "stop-tethering") + " 0\nexit\n");
                os.flush();
                os.close();
                return 1;
            } catch (Exception e) {
                // Root method failed
            }
        }

        if (Build.VERSION.SDK_INT >= 29) {
            return 5;
        }

        try {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = s;
            if (p != null && p.length() >= 8) {
                conf.preSharedKey = p;
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            }
            Method m = wm.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            Object res = m.invoke(wm, conf, en);
            if (res instanceof Boolean && (Boolean) res) return 4;
        } catch (Exception e) {
            // Fallback
        }

        if (Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 29) {
            return 2;
        }
        return 0;
    }

    @SuppressLint("MissingPermission")
    public void startLocalOnlyHotspot(final OnHotspotStateListener listener) {
        if (Build.VERSION.SDK_INT >= 26) {
            Handler handler = new Handler(Looper.getMainLooper());
            wm.startLocalOnlyHotspot(new LocalHotspotCallback(this, listener), handler);
        } else if (listener != null) {
            listener.onFailure(-1);
        }
    }

    public void stopLocalOnlyHotspot() {
        if (hotspotReservation != null) {
            hotspotReservation.close();
            hotspotReservation = null;
        }
    }

    public boolean isHotspotEnabled() {
        if (hotspotReservation != null) return true;
        try {
            Method m = wm.getClass().getDeclaredMethod("getWifiApState");
            Object res = m.invoke(wm);
            return res instanceof Integer && (Integer) res >= 12;
        } catch (Exception e) {
            return false;
        }
    }

    public void blockDevice(String mac, boolean b) {
        if (mac == null || !mac.contains(":")) return;
        String su = getBin("su");
        String ipt = getBin("iptables");
        if (su != null && ipt != null) {
            try {
                Process pr = new ProcessBuilder(su).start();
                DataOutputStream os = new DataOutputStream(pr.getOutputStream());
                os.writeBytes(ipt + " -" + (b ? "I" : "D") + " FORWARD -m mac --mac-source " + mac + " -j DROP\nexit\n");
                os.flush();
                os.close();
            } catch (Exception e) {
                // Block failed
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
                DataOutputStream os = new DataOutputStream(pr.getOutputStream());
                os.writeBytes(tc + " qdisc add dev wlan0 root handle 1: htb default 10\n");
                os.writeBytes(tc + " class add dev wlan0 parent 1: classid 1:1 htb rate " + k + "kbps ceil " + k + "kbps\n");
                os.writeBytes("exit\n");
                os.flush();
                os.close();
            } catch (Exception e) {
                // Limit failed
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
            listener.onStarted(config.SSID, config.preSharedKey);
        }
    }

    @Override
    public void onStopped() {
        manager.hotspotReservation = null;
        if (listener != null) listener.onStopped();
    }

    @Override
    public void onFailed(int reason) {
        if (listener != null) listener.onFailure(reason);
    }
}
