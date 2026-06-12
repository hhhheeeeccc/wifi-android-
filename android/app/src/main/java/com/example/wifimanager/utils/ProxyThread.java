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
            // Bind to the hotspot gateway IP (192.168.43.1) to ensure proxy connectivity only on the hotspot interface
            byte[] ipAddr = new byte[]{(byte)192, (byte)168, 43, 1};
            ServerSocket ss = new ServerSocket(8080, 50, InetAddress.getByAddress(ipAddr));
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
