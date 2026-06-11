package com.example.wifimanager.utils;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class ProxyThread extends Thread {
    private final ProxyManager pm;
    public ProxyThread(ProxyManager pm) { this.pm = pm; }
    @Override public void run() {
        try {
            pm.ss = new ServerSocket(8080);
            while (pm.run) {
                final Socket socket = pm.ss.accept();
                pm.submitTask(new HandlerThread(pm, socket));
            }
        } catch (IOException ignored) {
        } finally {
            pm.stopProxy();
        }
    }
}
