package com.example.wifimanager.repository;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.wifimanager.model.Device;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HotspotRepository {
    private final SharedPreferences p;
    private static final Set<String> activeIps = new HashSet<String>();

    public HotspotRepository(Context c) {
        this.p = c.getApplicationContext().getSharedPreferences("ds", Context.MODE_PRIVATE);
    }

    public static synchronized void registerIp(String ip) {
        if (ip != null) activeIps.add(ip);
    }

    public synchronized void saveDevice(Device d) {
        SharedPreferences.Editor e = p.edit();
        String m = d.getMacAddress();
        e.putLong(m + "l", d.getDataLimit());
        e.putLong(m + "u", d.getUsedData());
        e.putInt(m + "s", d.getSpeedLimit());
        e.putBoolean(m + "b", d.isBlocked());
        e.putString(m + "ip", d.getIpAddress());
        e.apply();
    }

    public synchronized void load(Device d) {
        String m = d.getMacAddress();
        d.setDataLimit(p.getLong(m + "l", 0));
        d.setUsedData(p.getLong(m + "u", 0));
        d.setSpeedLimit(p.getInt(m + "s", 0));
        d.setBlocked(p.getBoolean(m + "b", false));
    }

    public List<Device> getConnectedDevices() {
        List<Device> list = new ArrayList<Device>();
        BufferedReader br = null;
        Set<String> foundIps = new HashSet<String>();

        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(" +");
                if (s.length >= 4 && !s[0].equals("IP") && !s[3].equals("00:00:00:00:00:00")) {
                    Device d = new Device(s[0], s[3], "Device (" + s[0] + ")");
                    load(d);
                    list.add(d);
                    foundIps.add(s[0]);
                }
            }
        } catch (IOException e) {
            // Arp table not available
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {}
            }
        }

        // Fallback for Android 10+ where ARP is empty
        synchronized (activeIps) {
            for (String ip : activeIps) {
                if (!foundIps.contains(ip)) {
                    // Use IP as MAC for storage if MAC is unknown
                    Device d = new Device(ip, ip.replace(".", "_"), "Device (" + ip + ")");
                    load(d);
                    list.add(d);
                }
            }
        }

        return list;
    }
}
