package com.example.wifimanager.utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.io.DataOutputStream;
import java.lang.reflect.Method;

public class HotspotManager {
    private final WifiManager wifiManager;
    private final Context context;
    private WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    public HotspotManager(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public int setHotspotEnabled(boolean enabled, String ssid, String password) {
        if (RootUtils.isDeviceRooted()) {
            if (setHotspotEnabledRoot(enabled, ssid, password)) return 1; // ROOT_OK
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (setHotspotEnabledReflection(enabled)) return 2; // REFLECTION_OK

            if (enabled) {
                startLocalOnlyHotspot();
                return 3; // LOCAL_ONLY_STARTED
            } else {
                stopLocalOnlyHotspot();
                return 4; // STOPPED
            }
        } else {
            if (setHotspotEnabledLegacy(enabled, ssid, password)) return 5; // LEGACY_OK
        }
        return 0; // FAILED
    }

    private boolean setHotspotEnabledRoot(boolean enabled, String ssid, String password) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            if (enabled) {
                os.writeBytes("cmd tethering start-tethering 0\n");
                os.writeBytes("settings put global tether_offload_disabled 1\n");
            } else {
                os.writeBytes("cmd tethering stop-tethering 0\n");
                os.writeBytes("svc wifi disable-tethering\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setHotspotEnabledReflection(boolean enabled) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (enabled) {
                Class<?> callbackClass = Class.forName("android.net.ConnectivityManager");
                Method method = cm.getClass().getDeclaredMethod("startTethering", int.class, boolean.class, callbackClass, Handler.class);
                method.setAccessible(true);
                method.invoke(cm, 0, false, null, new Handler(Looper.getMainLooper()));
            } else {
                Method method = cm.getClass().getDeclaredMethod("stopTethering", int.class);
                method.setAccessible(true);
                method.invoke(cm, 0);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setHotspotEnabledLegacy(boolean enabled, String ssid, String password) {
        try {
            if (enabled) wifiManager.setWifiEnabled(false);
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
            try {
                wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                        super.onStarted(reservation);
                        hotspotReservation = reservation;
                        Log.d("HotspotManager", "LocalOnlyHotspot started: " + reservation.getWifiConfiguration().SSID);
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        hotspotReservation = null;
                    }

                    @Override
                    public void onFailed(int reason) {
                        super.onFailed(reason);
                        hotspotReservation = null;
                    }
                }, new Handler(Looper.getMainLooper()));
            } catch (Exception e) {
                Log.e("HotspotManager", "Failed to start LocalOnlyHotspot", e);
            }
        }
    }

    public void stopLocalOnlyHotspot() {
        if (hotspotReservation != null) {
            hotspotReservation.close();
            hotspotReservation = null;
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
            return state == 13 || state == 12 || hotspotReservation != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void blockDevice(String mac, boolean block) {
        if (!RootUtils.isDeviceRooted()) return;
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            if (block) {
                os.writeBytes("iptables -I FORWARD -m mac --mac-source " + mac + " -j DROP\n");
            } else {
                os.writeBytes("iptables -D FORWARD -m mac --mac-source " + mac + " -j DROP\n");
            }
            os.writeBytes("exit\n");
            os.flush();
        } catch (Exception e) {
            Log.e("HotspotManager", "Error blocking device", e);
        }
    }

    public void limitSpeed(String mac, int kbps) {
        if (!RootUtils.isDeviceRooted()) return;
        // Simplified tc command for speed limiting
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("tc qdisc add dev wlan0 root handle 1: htb default 10\n");
            os.writeBytes("tc class add dev wlan0 parent 1: classid 1:1 htb rate " + kbps + "kbps ceil " + kbps + "kbps\n");
            os.writeBytes("tc filter add dev wlan0 protocol ip parent 1:0 prio 1 u32 match ip src 0.0.0.0/0 flowid 1:1\n");
            os.writeBytes("exit\n");
            os.flush();
        } catch (Exception e) {
            Log.e("HotspotManager", "Error limiting speed", e);
        }
    }
}
