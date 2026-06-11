package com.example.wifimanager.utils;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class HandlerThread extends Thread {
    private final ProxyManager pm; private final Socket s;
    public HandlerThread(ProxyManager pm, Socket s) { this.pm = pm; this.s = s; }
    @Override public void run() {
        String ip = s.getInetAddress().getHostAddress();
        if (pm.isBlk(ip)) { try { s.close(); } catch (IOException e) {} return; }

        Socket r = null;
        try {
            InputStream in = s.getInputStream(); OutputStream out = s.getOutputStream();
            byte[] b = new byte[8192]; int n = in.read(b); if (n <= 0) return;
            String h = new String(b, 0, n); String host = null;
            for (String l : h.split("\r\n")) { if (l.toLowerCase().startsWith("host:")) host = l.substring(5).trim().split(":")[0]; }
            if (host == null) return;

            r = new Socket(host, h.contains("CONNECT") ? 443 : 80);
            if (h.contains("CONNECT")) { out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes()); out.flush(); }
            else { r.getOutputStream().write(b, 0, n); }

            PThread t1 = new PThread(ip, in, r.getOutputStream(), pm, s, r);
            PThread t2 = new PThread(ip, r.getInputStream(), out, pm, s, r);
            t1.start(); t2.start();
        } catch (Exception e) {
            try { s.close(); } catch (IOException ignored) {}
            try { if (r != null) r.close(); } catch (IOException ignored) {}
        }
    }
}
