package com.example.wifimanager.utils;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class HotspotManager {
    private final Context ctx;
    private final WifiManager wm;
    private static final List<String> BIN_PATHS = Arrays.asList("/system/bin/", "/system/xbin/", "/sbin/");

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
        String su = getBin("su");
        if (su != null) { try {
            Process pr = new ProcessBuilder(su).start();
            DataOutputStream os = new DataOutputStream(pr.getOutputStream());
            os.writeBytes("cmd tethering " + (en ? "start-tethering" : "stop-tethering") + " 0\nexit\n");
            os.flush();
            os.close();
            return 1;
        } catch (Exception ignored) {} }
        if (Build.VERSION.SDK_INT >= 26) return 2;
        try {
            if (en) wm.setWifiEnabled(false);
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = s;
            if (p != null && p.length() >= 8) {
                conf.preSharedKey = p;
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            }
            Method m = wm.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            Object res = m.invoke(wm, conf, en);
            return (res instanceof Boolean && (Boolean)res) ? 4 : 0;
        } catch (Exception ignored) { return 0; }
    }

    public boolean isHotspotEnabled() {
        try {
            Method m = wm.getClass().getDeclaredMethod("getWifiApState");
            Object res = m.invoke(wm);
            return res instanceof Integer && (Integer)res >= 12;
        } catch (Exception ignored) { return false; }
    }

    public void blockDevice(String mac, boolean b) {
        if (mac == null || !mac.contains(":")) return;
        String su = getBin("su");
        String ipt = getBin("iptables");
        if (su != null && ipt != null) { try {
            Process pr = new ProcessBuilder(su).start();
            DataOutputStream os = new DataOutputStream(pr.getOutputStream());
            os.writeBytes(ipt + " -" + (b ? "I" : "D") + " FORWARD -m mac --mac-source " + mac + " -j DROP\nexit\n");
            os.flush();
            os.close();
        } catch (Exception ignored) {} }
    }

    public void limitSpeed(String mac, int k) {
        String su = getBin("su");
        String tc = getBin("tc");
        if (su != null && tc != null) { try {
            Process pr = new ProcessBuilder(su).start();
            DataOutputStream os = new DataOutputStream(pr.getOutputStream());
            os.writeBytes(tc + " qdisc add dev wlan0 root handle 1: htb default 10\nexit\n");
            os.flush();
            os.close();
        } catch (Exception ignored) {} }
    }
}
