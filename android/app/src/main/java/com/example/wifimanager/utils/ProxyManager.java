package com.example.wifimanager.utils;
import android.content.Context;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyManager {
    private final HotspotRepository repo;
    ServerSocket ss;
    volatile boolean run = false;
    private final Map<String, Long> usageMap = new ConcurrentHashMap<String, Long>();
    private final Map<String, Boolean> blockMap = new ConcurrentHashMap<String, Boolean>();
    private final Map<String, Integer> speedMap = new ConcurrentHashMap<String, Integer>();
    private volatile long lastUpdate = 0;
    private ExecutorService threadPool;

    public ProxyManager(Context c) { this.repo = new HotspotRepository(c); }

    public boolean isRunning() { return run; }

    public synchronized void startProxy() {
        if (!run) {
            run = true;
            threadPool = Executors.newCachedThreadPool();
            new ProxyThread(this).start();
        }
    }

    public synchronized void stopProxy() {
        run = false;
        if (ss != null) { try { ss.close(); } catch (IOException ignored) {} ss = null; }
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    public void submitTask(Runnable task) {
        ExecutorService pool = threadPool;
        if (pool != null && !pool.isShutdown()) {
            pool.execute(task);
        }
    }

    public void addUsage(String ip, int n) {
        if (ip == null) return;
        Long current = usageMap.get(ip);
        usageMap.put(ip, (current == null ? 0L : current) + n);
    }

    public long getAndResetUsage(String ip) {
        if (ip == null) return 0L;
        Long v = usageMap.remove(ip);
        return v == null ? 0L : v;
    }

    public boolean isIpBlocked(String ip) {
        if (ip == null) return false;
        updateCache();
        Boolean b = blockMap.get(ip);
        return b != null && b;
    }

    public int getSpeedLimit(String ip) {
        if (ip == null) return 0;
        updateCache();
        Integer s = speedMap.get(ip);
        return s == null ? 0 : s;
    }

    private void updateCache() {
        if (System.currentTimeMillis() - lastUpdate < 5000) return;
        synchronized(this) {
            if (System.currentTimeMillis() - lastUpdate < 5000) return;
            List<Device> ds = repo.getConnectedDevices();
            blockMap.clear(); speedMap.clear();
            for (Device d : ds) {
                String ip = d.getIpAddress();
                if (ip != null) {
                    blockMap.put(ip, d.isBlocked());
                    speedMap.put(ip, d.getSpeedLimit());
                }
            }
            lastUpdate = System.currentTimeMillis();
        }
    }

    public String parseHost(String header) {
        if (header == null) return null;
        String[] lines = header.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("host:")) {
                String val = line.substring(5).trim();
                return val.split(":")[0];
            }
        }
        return null;
    }
}
