package com.example.wifimanager.utils;
import java.io.InputStream;
import java.io.OutputStream;
public class PThread extends Thread {
    private final String ip; private final InputStream in; private final OutputStream out; private final ProxyManager pm;
    public PThread(String ip, InputStream in, OutputStream out, ProxyManager pm) { this.ip = ip; this.in = in; this.out = out; this.pm = pm; }
    @Override public void run() {
        byte[] b = new byte[4096]; int n;
        try { while (pm.run && (n = in.read(b)) != -1) {
            int s = pm.getSpd(ip); if (s > 0) Thread.sleep((n * 8L) / s);
            out.write(b, 0, n); out.flush(); pm.addUse(ip, n);
            if (pm.isBlk(ip)) break;
        } } catch (Exception e) {}
    }
}
