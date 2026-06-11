package com.example.wifimanager.utils;
import java.net.ServerSocket;
public class ProxyThread extends Thread {
    private final ProxyManager pm;
    public ProxyThread(ProxyManager pm) { this.pm = pm; }
    @Override public void run() {
        try { pm.ss = new ServerSocket(8080); while (pm.run) { new HandlerThread(pm, pm.ss.accept()).start(); } } catch (Exception e) {}
    }
}
