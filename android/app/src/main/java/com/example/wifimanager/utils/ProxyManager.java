package com.example.wifimanager.utils;
import android.content.Context;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
public class ProxyManager {
    public final HotspotRepository repo;
    public ServerSocket ss;
    public boolean run = false;
    public final Map<String, Long> use = new HashMap<String, Long>();
    public final Map<String, Boolean> blk = new HashMap<String, Boolean>();
    public final Map<String, Integer> spd = new HashMap<String, Integer>();
    public long last = 0;
    public ProxyManager(Context c) { this.repo = new HotspotRepository(c); }
    public synchronized void startProxy() { if (!run) { run = true; new ProxyThread(this).start(); } }
    public synchronized void stopProxy() { run = false; try { if (ss != null) ss.close(); } catch (Exception e) {} }
    public synchronized void addUse(String ip, int n) { Long c = use.get(ip); use.put(ip, (c == null ? 0L : c) + n); }
    public synchronized long getUse(String ip) { Long v = use.remove(ip); return v == null ? 0L : v; }
    public synchronized boolean isBlk(String ip) { update(); Boolean b = blk.get(ip); return b != null && b; }
    public synchronized int getSpd(String ip) { update(); Integer s = spd.get(ip); return s == null ? 0 : s; }
    private void update() {
        if (System.currentTimeMillis() - last < 5000) return;
        List<Device> ds = repo.getConnectedDevices();
        blk.clear(); spd.clear();
        for (Device d : ds) { blk.put(d.getIpAddress(), d.isBlocked()); spd.put(d.getIpAddress(), d.getSpeedLimit()); }
        last = System.currentTimeMillis();
    }
}
