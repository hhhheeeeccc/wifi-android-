package com.example.wifimanager.utils;
import android.content.Context;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import java.net.ServerSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyManager {
    private final HotspotRepository repo;
    private ServerSocket serverSocket;
    private volatile boolean runStatus = false;
    private final Map<String, Long> usageMap = new ConcurrentHashMap<String, Long>();
    private final Map<String, Boolean> blockMap = new ConcurrentHashMap<String, Boolean>();
    private final Map<String, Integer> speedMap = new ConcurrentHashMap<String, Integer>();
    private volatile long lastUpdateTimestamp = 0;
    private ExecutorService threadPool;
    private String hostIp = "192.168.43.1";

    public ProxyManager(Context c) { this.repo = new HotspotRepository(c); }

    public boolean isRunning() { return runStatus; }

    public synchronized void startProxy() {
        if (!runStatus) {
            runStatus = true;
            threadPool = Executors.newCachedThreadPool();
            new ProxyThread(this).start();
        }
    }

    public synchronized void stopProxy() {
        runStatus = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Squelch close error
            }
            serverSocket = null;
        }
        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
    }

    public void setServerSocket(ServerSocket socket) { this.serverSocket = socket; }

    public ServerSocket getServerSocket() { return serverSocket; }

    public void setHostIp(String ip) { this.hostIp = ip; }
    public String getHostIp() { return hostIp; }

    public void refreshHostIp() {
        InetAddress addr = getHotspotAddress();
        if (addr != null) {
            this.hostIp = addr.getHostAddress();
        }
    }

    public InetAddress getHotspotAddress() {
        InetAddress best = null;
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                String name = intf.getName().toLowerCase();
                // Prioritize p2p and ap interfaces
                if (name.contains("p2p") || name.contains("ap") || name.contains("wlan") || name.contains("softap")) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                            String sAddr = addr.getHostAddress();
                            // Hotspot IPs usually end in .1
                            if (sAddr.endsWith(".1")) {
                                if (sAddr.equals("192.168.49.1") || sAddr.equals("192.168.43.1")) return addr;
                                if (best == null) best = addr;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return best;
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
        if (System.currentTimeMillis() - lastUpdateTimestamp < 5000) return;
        synchronized(this) {
            if (System.currentTimeMillis() - lastUpdateTimestamp < 5000) return;
            List<Device> ds = repo.getConnectedDevices();
            blockMap.clear();
            speedMap.clear();
            for (Device d : ds) {
                String ip = d.getIpAddress();
                if (ip != null) {
                    blockMap.put(ip, d.isBlocked());
                    speedMap.put(ip, d.getSpeedLimit());
                }
            }
            lastUpdateTimestamp = System.currentTimeMillis();
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
