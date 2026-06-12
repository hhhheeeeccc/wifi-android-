package com.example.wifimanager.utils;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class ProxyThread extends Thread {
    private final ProxyManager pm;
    public ProxyThread(ProxyManager pm) { this.pm = pm; }
    @Override public void run() {
        try {
            // Bind to the specific hotspot gateway IP to resolve security hotspot and ensure connectivity
            ServerSocket ss = new ServerSocket(8080, 50, InetAddress.getByName("192.168.43.1"));
            pm.setServerSocket(ss);
            while (pm.isRunning()) {
                final Socket socket = ss.accept();
                pm.submitTask(new HandlerThread(pm, socket));
            }
        } catch (IOException e) {
            // Socket likely closed
        } finally {
            pm.stopProxy();
        }
    }
}
