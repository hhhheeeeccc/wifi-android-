package com.example.wifimanager.repository;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.wifimanager.model.Device;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HotspotRepository {
    private final SharedPreferences p;
    public HotspotRepository(Context c) {
        this.p = c.getApplicationContext().getSharedPreferences("ds", Context.MODE_PRIVATE);
    }

    public synchronized void saveDevice(Device d) {
        SharedPreferences.Editor e = p.edit();
        String m = d.getMacAddress();
        e.putLong(m + "l", d.getDataLimit());
        e.putLong(m + "u", d.getUsedData());
        e.putInt(m + "s", d.getSpeedLimit());
        e.putBoolean(m + "b", d.isBlocked());
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
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] s = line.split(" +");
                // /proc/net/arp: IP, HW Type, Flags, MAC, Mask, Interface
                if (s.length >= 6) {
                    String ip = s[0];
                    String flags = s[2];
                    String mac = s[3];
                    String iface = s[5];

                    if (!ip.equals("IP") && !flags.equals("0x0") && !mac.equals("00:00:00:00:00:00")
                        && (iface.contains("wlan") || iface.contains("ap"))) {
                        Device d = new Device(ip, mac, "Device (" + ip + ")");
                        load(d);
                        list.add(d);
                    }
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
        return list;
    }
}
