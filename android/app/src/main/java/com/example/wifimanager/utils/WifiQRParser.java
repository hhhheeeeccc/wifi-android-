package com.example.wifimanager.utils;

public class WifiQRParser {
    public static class WifiData {
        public String ssid = "";
        public String password = "";
        public String proxyHost = "";
        public int proxyPort = 8080;
    }

    public static WifiData parse(String data) {
        if (data == null || !data.startsWith("WIFI:")) return null;
        WifiData wifi = new WifiData();
        String[] parts = data.substring(5).split(";");
        for (String part : parts) {
            if (part.startsWith("S:")) wifi.ssid = part.substring(2);
            else if (part.startsWith("P:")) wifi.password = part.substring(2);
            else if (part.startsWith("PH:")) wifi.proxyHost = part.substring(3);
            else if (part.startsWith("PP:")) {
                try {
                    wifi.proxyPort = Integer.parseInt(part.substring(3));
                } catch (NumberFormatException e) {
                    wifi.proxyPort = 8080;
                }
            }
        }
        return wifi;
    }
}
