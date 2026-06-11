package com.example.wifimanager.utils;
import java.io.File;
public class RootUtils {
    public static boolean isDeviceRooted() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su"};
        for (String p : paths) { if (new File(p).exists()) return true; }
        return false;
    }
}
