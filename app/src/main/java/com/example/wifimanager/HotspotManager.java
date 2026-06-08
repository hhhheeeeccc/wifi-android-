package com.example.wifimanager;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import java.io.DataOutputStream;
import java.lang.reflect.Method;

public class HotspotManager {
    private final WifiManager wifiManager;
    private final Context context;

    public HotspotManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public boolean setHotspotEnabled(boolean enabled, String ssid, String password) {
        if (RootUtils.isDeviceRooted()) {
            return setHotspotEnabledRoot(enabled, ssid, password);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Non-rooted Android 8.0+
            Log.d("HotspotManager", "Android 8.0+ No Root. Fallback to LocalOnly or Manual.");
            return false;
        } else {
            // Older Android versions
            return setHotspotEnabledLegacy(enabled, ssid, password);
        }
    }

    private boolean setHotspotEnabledRoot(boolean enabled, String ssid, String password) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            if (enabled) {
                // Command to enable tethering via service call or settings put
                // This varies by Android version, but common one is 'svc wifi disable' then 'service call connectivity 24 i32 1' (for example)
                os.writeBytes("settings put global tether_offload_disabled 1\n");
                // Simplified: use 'svc data' or similar to ensure mobile data is on for sharing
                Log.d("HotspotManager", "Root: Enabling hotspot via shell...");
            } else {
                os.writeBytes("svc wifi disable-tethering\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setHotspotEnabledLegacy(boolean enabled, String ssid, String password) {
        try {
            if (enabled) {
                wifiManager.setWifiEnabled(false);
            }
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = ssid;
            if (password != null && password.length() >= 8) {
                wifiConfig.preSharedKey = password;
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            } else {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean) method.invoke(wifiManager, wifiConfig, enabled);
        } catch (Exception e) {
            return false;
        }
    }

    public void startLocalOnlyHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // This would normally use a callback. Simplified for logic flow.
            Log.d("HotspotManager", "Starting LocalOnlyHotspot for file sharing...");
        }
    }

    public void openHotspotSettings() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            context.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public boolean isHotspotEnabled() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
            int state = (Integer) method.invoke(wifiManager);
            return state == 13 || state == 12;
        } catch (Exception e) {
            return false;
        }
    }
}
