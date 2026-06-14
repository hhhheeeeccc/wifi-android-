package com.example.wifimanager.utils;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ProxyThread extends Thread {
    private final ProxyManager pm;
    public ProxyThread(ProxyManager pm) { this.pm = pm; }

    private InetAddress getHotspotAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                // Common names for hotspot interfaces: wlan0, ap0, softap0, p2p-wlan0-0
                if (intf.getName().contains("wlan") || intf.getName().contains("ap")) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                            // On many Androids, the hotspot IP ends with .1
                            String sAddr = addr.getHostAddress();
                            if (sAddr.endsWith(".1")) {
                                return addr;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            return InetAddress.getByAddress(new byte[]{(byte)192, (byte)168, 43, 1});
        } catch (Exception e) {
            return null;
        }
    }

    @Override public void run() {
        try {
            InetAddress addr = getHotspotAddress();
            if (addr != null) {
                pm.setHostIp(addr.getHostAddress());
                ServerSocket ss = new ServerSocket(8282, 50, addr);
                pm.setServerSocket(ss);
                while (pm.isRunning()) {
                    final Socket socket = ss.accept();
                    pm.submitTask(new HandlerThread(pm, socket));
                }
            }
        } catch (IOException e) {
            // Socket likely closed
        } finally {
            pm.stopProxy();
        }
    }
}
