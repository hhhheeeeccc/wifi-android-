package com.example.wifimanager;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

public class HotspotManager {
    private final WifiManager wifiManager;
    private final Context context;

    public HotspotManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public boolean setHotspotEnabled(boolean enabled, String ssid, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Restrictions on Android 8.0+
            Log.d("HotspotManager", "Android 8.0+ detected. Redirecting to settings.");
            return false;
        } else {
            try {
                if (enabled) {
                    wifiManager.setWifiEnabled(false);
                }

                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = ssid;
                if (password != null && password.length() >= 8) {
                    wifiConfig.preSharedKey = password;
                    wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                } else {
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }

                Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                return (Boolean) method.invoke(wifiManager, wifiConfig, enabled);
            } catch (Exception e) {
                Log.e("HotspotManager", "Error setting hotspot", e);
                return false;
            }
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
