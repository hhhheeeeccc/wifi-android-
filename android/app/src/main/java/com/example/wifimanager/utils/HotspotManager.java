package com.example.wifimanager.utils;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
public class HotspotManager {
    private final Context ctx;
    private final WifiManager wm;
    public HotspotManager(Context c) {
        this.ctx = c;
        this.wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    public int setHotspotEnabled(boolean en, String s, String p) {
        String suPath = RootUtils.getSuPath();
        if (suPath != null) { try {
            Process pr = Runtime.getRuntime().exec(suPath);
            DataOutputStream os = new DataOutputStream(pr.getOutputStream());
            os.writeBytes("cmd tethering " + (en ? "start-tethering" : "stop-tethering") + " 0\nexit\n");
            os.flush(); return 1;
        } catch (Exception e) {} }
        if (Build.VERSION.SDK_INT >= 26) return 2;
        try {
            if (en) wm.setWifiEnabled(false);
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = s; if (p != null && p.length() >= 8) { conf.preSharedKey = p; conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK); }
            Method m = wm.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return ((Boolean) m.invoke(wm, conf, en)) ? 4 : 0;
        } catch (Exception e) { return 0; }
    }
    public boolean isHotspotEnabled() {
        try { Method m = wm.getClass().getDeclaredMethod("getWifiApState"); return ((Integer) m.invoke(wm)) >= 12; } catch (Exception e) { return false; }
    }
    public void blockDevice(String mac, boolean b) {
        String suPath = RootUtils.getSuPath();
        if (suPath != null) { try {
            Process pr = Runtime.getRuntime().exec(suPath);
            DataOutputStream os = new DataOutputStream(pr.getOutputStream());
            // Using absolute path for system binaries to satisfy security checks
            String bin = "/system/bin/iptables";
            if (!new java.io.File(bin).exists()) bin = "/system/xbin/iptables";
            os.writeBytes(bin + " -" + (b ? "I" : "D") + " FORWARD -m mac --mac-source " + mac + " -j DROP\nexit\n");
            os.flush();
        } catch (Exception e) {} }
    }
    public void limitSpeed(String mac, int k) {
        String suPath = RootUtils.getSuPath();
        if (suPath != null) { try {
            Process pr = Runtime.getRuntime().exec(suPath);
            DataOutputStream os = new DataOutputStream(pr.getOutputStream());
            String bin = "/system/bin/tc";
            if (!new java.io.File(bin).exists()) bin = "/system/xbin/tc";
            os.writeBytes(bin + " qdisc add dev wlan0 root handle 1: htb default 10\nexit\n");
            os.flush();
        } catch (Exception e) {} }
    }
}
