package com.example.wifimanager.utils;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.io.IOException;

public class PThread extends Thread {
    private final String ip; private final InputStream in; private final OutputStream out; private final ProxyManager pm;
    private final Socket s1, s2;
    public PThread(String ip, InputStream in, OutputStream out, ProxyManager pm, Socket s1, Socket s2) {
        this.ip = ip; this.in = in; this.out = out; this.pm = pm; this.s1 = s1; this.s2 = s2;
    }
    @Override public void run() {
        byte[] b = new byte[4096]; int n;
        try {
            while (pm.isRunning() && (n = in.read(b)) != -1) {
                int s = pm.getSpd(ip); if (s > 0) Thread.sleep((n * 8L) / s);
                out.write(b, 0, n); out.flush(); pm.addUse(ip, n);
                if (pm.isBlk(ip)) break;
            }
        } catch (Exception e) {
        } finally {
            try { s1.close(); } catch (IOException ignored) {}
            try { s2.close(); } catch (IOException ignored) {}
        }
    }
}
