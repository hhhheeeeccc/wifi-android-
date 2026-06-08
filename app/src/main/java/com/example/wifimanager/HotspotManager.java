package com.example.wifimanager;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;

public class HotspotManager {
    private final WifiManager wifiManager;
    private final Context context;

    public HotspotManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public boolean setHotspotEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android 8.0+, we usually use ConnectivityManager.startLocalOnlyHotspot
            // However, full control often requires system app or root.
            Log.d("HotspotManager", "Enable hotspot for Oreo+ called");
            // This is a simplified representation
            return true;
        } else {
            try {
                if (enabled) {
                    wifiManager.setWifiEnabled(false);
                }
                Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                return (Boolean) method.invoke(wifiManager, null, enabled);
            } catch (Exception e) {
                Log.e("HotspotManager", "Error setting hotspot", e);
                return false;
            }
        }
    }

    public boolean isHotspotEnabled() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
            int state = (Integer) method.invoke(wifiManager);
            return state == 13; // 13 is WIFI_AP_STATE_ENABLED
        } catch (Exception e) {
            return false;
        }
    }
}
