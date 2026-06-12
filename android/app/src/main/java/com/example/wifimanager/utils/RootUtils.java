package com.example.wifimanager.utils;
import java.io.File;
public class RootUtils {
    public static boolean isDeviceRooted() {
        return getSuPath() != null;
    }
    public static String getSuPath() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su"};
        for (String p : paths) { if (new File(p).exists()) return p; }
        return null;
    }
}
