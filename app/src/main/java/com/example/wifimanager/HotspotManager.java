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

    /**
     * تفعيل أو إيقاف نقطة الاتصال.
     * ملاحظة: في الإصدارات الأحدث من أندرويد (8.0+)، يتطلب التحكم الكامل في نقطة الاتصال
     * أن يكون التطبيق من تطبيقات النظام أو لديه صلاحيات خاصة.
     */
    public boolean setHotspotEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // لنظام أندرويد 8.0 فما فوق، التفعيل البرمجي المباشر مقيد لأسباب أمنية.
            // يفضل توجيه المستخدم لإعدادات النظام أو استخدام API خاص إذا كان التطبيق سيُوقع كـ System App.
            Log.d("HotspotManager", "Android 8.0+ detected. Manual intervention or system privileges required.");
            return false;
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
            // WIFI_AP_STATE_ENABLING = 12, WIFI_AP_STATE_ENABLED = 13
            return state == 13 || state == 12;
        } catch (Exception e) {
            return false;
        }
    }
}
