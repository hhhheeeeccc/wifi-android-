package com.example.wifimanager.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RootUtils {
    public static boolean isDeviceRooted() {
        List<String> paths = Arrays.asList(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        );
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
}
