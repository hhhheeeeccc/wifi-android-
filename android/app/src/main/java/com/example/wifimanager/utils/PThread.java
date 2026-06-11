package com.example.wifimanager.utils;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.io.IOException;

public class PThread implements Runnable {
    private final String ip; private final InputStream inputStream; private final OutputStream outputStream; private final ProxyManager proxyManager;
    private final Socket s1, s2;
    public PThread(String ip, InputStream in, OutputStream out, ProxyManager pm, Socket s1, Socket s2) {
        this.ip = ip; this.inputStream = in; this.outputStream = out; this.proxyManager = pm; this.s1 = s1; this.s2 = s2;
    }
    @Override public void run() {
        byte[] buffer = new byte[4096]; int n;
        try {
            while (proxyManager.isRunning() && (n = inputStream.read(buffer)) != -1) {
                int speed = proxyManager.getSpeedLimit(ip);
                if (speed > 0) { try { Thread.sleep((n * 8L) / speed); } catch (InterruptedException ignored) { break; } }
                outputStream.write(buffer, 0, n);
                outputStream.flush();
                proxyManager.addUsage(ip, n);
                if (proxyManager.isIpBlocked(ip)) break;
            }
        } catch (IOException ignored) {
        } finally {
            try { if (s1 != null) s1.close(); } catch (IOException ignored) {}
            try { if (s2 != null) s2.close(); } catch (IOException ignored) {}
        }
    }
}
