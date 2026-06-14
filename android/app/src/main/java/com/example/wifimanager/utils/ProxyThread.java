package com.example.wifimanager.utils;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class ProxyThread extends Thread {
    private final ProxyManager pm;
    public ProxyThread(ProxyManager pm) { this.pm = pm; }

    private static class RefreshTask implements Runnable {
        private final ProxyManager pm;
        RefreshTask(ProxyManager pm) { this.pm = pm; }
        @Override public void run() {
            while (pm.isRunning()) {
                pm.refreshHostIp();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override public void run() {
        try {
            // Bind to all interfaces (0.0.0.0) on port 8080 to ensure the proxy starts
            // even before the hotspot interface is fully initialized.
            ServerSocket ss = new ServerSocket(8080);
            pm.setServerSocket(ss);

            // Periodically refresh the Host IP to ensure UI shows correct information
            pm.submitTask(new RefreshTask(pm));

            while (pm.isRunning()) {
                final Socket socket = ss.accept();
                // When a connection is accepted, we ensure the client's IP is registered
                // so it appears in the device list.
                pm.submitTask(new HandlerThread(pm, socket));
            }
        } catch (IOException e) {
            // Socket likely closed or binding failed
        } finally {
            pm.stopProxy();
        }
    }
}
