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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyManager {
    public static final String IP_STANDARD_AP = "192.168.43.1";
    public static final String IP_LOCAL_ONLY = "192.168.49.1";

    private final HotspotRepository repo;
    private ServerSocket serverSocket;
    private volatile boolean runStatus = false;
    private final Map<String, Long> usageMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> blockMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> speedMap = new ConcurrentHashMap<>();
    private volatile long lastUpdateTimestamp = 0;
    private ExecutorService threadPool;
    private String hostIp = IP_STANDARD_AP;

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
                InetAddress found = findAddressInInterface(intf);
                if (found != null) {
                    String sAddr = found.getHostAddress();
                    if (IP_LOCAL_ONLY.equals(sAddr) || IP_STANDARD_AP.equals(sAddr)) {
                        return found;
                    }
                    if (best == null) best = found;
                }
            }
        } catch (Exception ignored) {}
        return best;
    }

    private InetAddress findAddressInInterface(NetworkInterface intf) {
        String name = intf.getName().toLowerCase();
        if (!isHotspotInterface(name)) return null;

        List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
        for (InetAddress addr : addrs) {
            if (isIpv4Address(addr)) {
                String sAddr = addr.getHostAddress();
                if (sAddr.endsWith(".1")) {
                    return addr;
                }
            }
        }
        return null;
    }

    private boolean isHotspotInterface(String name) {
        return name.contains("p2p") || name.contains("ap") || name.contains("wlan") || name.contains("softap");
    }

    private boolean isIpv4Address(InetAddress addr) {
        return !addr.isLoopbackAddress() && addr.getAddress().length == 4;
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
