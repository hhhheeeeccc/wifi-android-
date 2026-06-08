package com.example.wifimanager.repository;

import android.content.Context;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.utils.HotspotManager;
import com.example.wifimanager.utils.ProxyManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class HotspotRepository {
    private final HotspotManager hotspotManager;
    private final ProxyManager proxyManager;

    public HotspotRepository(Context context) {
        this.hotspotManager = new HotspotManager(context);
        this.proxyManager = new ProxyManager();
    }

    public boolean toggleHotspot(boolean enable, String ssid, String password) {
        return hotspotManager.setHotspotEnabled(enable, ssid, password);
    }

    public void toggleProxy(boolean enable) {
        if (enable) proxyManager.startProxy();
        else proxyManager.stopProxy();
    }

    public boolean isHotspotActive() {
        return hotspotManager.isHotspotEnabled();
    }

    public void openSettings() {
        hotspotManager.openHotspotSettings();
    }

    public List<Device> getConnectedDevices() {
        List<Device> devices = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted.length >= 4 && !splitted[0].equals("IP")) {
                    String ip = splitted[0];
                    String mac = splitted[3];
                    if (!mac.equals("00:00:00:00:00:00")) {
                        devices.add(new Device(ip, mac, "جهاز متصل (" + ip + ")"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return devices;
    }
}
