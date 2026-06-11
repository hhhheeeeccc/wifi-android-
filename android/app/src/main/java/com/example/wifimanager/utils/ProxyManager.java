package com.example.wifimanager.utils;
import android.content.Context;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyManager {
    public final HotspotRepository repo;
    public ServerSocket ss;
    public volatile boolean run = false;
    private final Map<String, Long> use = new ConcurrentHashMap<String, Long>();
    private final Map<String, Boolean> blk = new ConcurrentHashMap<String, Boolean>();
    private final Map<String, Integer> spd = new ConcurrentHashMap<String, Integer>();
    private volatile long last = 0;

    public ProxyManager(Context c) { this.repo = new HotspotRepository(c); }
    public boolean isRunning() { return run; }
    public synchronized void startProxy() { if (!run) { run = true; new ProxyThread(this).start(); } }
    public synchronized void stopProxy() { run = false; try { if (ss != null) ss.close(); } catch (Exception e) {} }

    public void addUse(String ip, int n) {
        Long c = use.get(ip);
        use.put(ip, (c == null ? 0L : c) + n);
    }

    public long getUse(String ip) {
        Long v = use.remove(ip);
        return v == null ? 0L : v;
    }

    public boolean isBlk(String ip) {
        update();
        Boolean b = blk.get(ip);
        return b != null && b;
    }

    public int getSpd(String ip) {
        update();
        Integer s = spd.get(ip);
        return s == null ? 0 : s;
    }

    private void update() {
        if (System.currentTimeMillis() - last < 5000) return;
        synchronized(this) {
            if (System.currentTimeMillis() - last < 5000) return;
            List<Device> ds = repo.getConnectedDevices();
            blk.clear(); spd.clear();
            for (Device d : ds) {
                blk.put(d.getIpAddress(), d.isBlocked());
                spd.put(d.getIpAddress(), d.getSpeedLimit());
            }
            last = System.currentTimeMillis();
        }
    }

    public String parseHost(String h) {
        String[] lines = h.split("\r\n");
        for (String l : lines) {
            if (l.toLowerCase().startsWith("host:")) return l.substring(5).trim().split(":")[0];
        }
        return null;
    }
}
