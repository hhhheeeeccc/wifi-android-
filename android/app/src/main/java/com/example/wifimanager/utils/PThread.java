package com.example.wifimanager.utils;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.io.IOException;

public class PThread implements Runnable {
    private final String ip;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ProxyManager proxyManager;
    private final Socket s1;
    private final Socket s2;
    private static final int BUFFER_SIZE = 65536;

    public PThread(String ip, InputStream in, OutputStream out, ProxyManager pm, Socket s1, Socket s2) {
        this.ip = ip;
        this.inputStream = in;
        this.outputStream = out;
        this.proxyManager = pm;
        this.s1 = s1;
        this.s2 = s2;
    }
    @Override public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (proxyManager.isRunning()) {
                int n = inputStream.read(buffer);
                if (n == -1) break;

                if (proxyManager.isIpBlocked(ip)) {
                    break;
                }

                int speed = proxyManager.getSpeedLimit(ip);
                if (speed > 0) {
                    try {
                        long waitTime = (n * 8L) / speed;
                        if (waitTime > 0) Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                outputStream.write(buffer, 0, n);
                outputStream.flush();
                proxyManager.addUsage(ip, n);
            }
        } catch (IOException e) {
            // Connection closed or error in piping
        } finally {
            closeAll();
        }
    }

    private void closeAll() {
        try { if (s1 != null && !s1.isClosed()) s1.close(); } catch (IOException ignored) {}
        try { if (s2 != null && !s2.isClosed()) s2.close(); } catch (IOException ignored) {}
    }
}
