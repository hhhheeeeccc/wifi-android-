package com.example.wifimanager.utils;
import com.example.wifimanager.repository.HotspotRepository;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class HandlerThread implements Runnable {
    private final ProxyManager pm;
    private final Socket clientSocket;
    public HandlerThread(ProxyManager pm, Socket s) {
        this.pm = pm;
        this.clientSocket = s;
    }
    @Override public void run() {
        if (clientSocket == null || clientSocket.getInetAddress() == null) return;
        String ip = clientSocket.getInetAddress().getHostAddress();

        // Register IP to show in device list
        HotspotRepository.registerIp(ip);

        if (pm.isIpBlocked(ip)) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore error on close
            }
            return;
        }

        Socket remoteSocket = null;
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            byte[] buffer = new byte[8192];
            int n = in.read(buffer);
            if (n <= 0) {
                clientSocket.close();
                return;
            }

            String header = new String(buffer, 0, n);
            String firstLine = header.split("\r\n")[0];
            String[] parts = firstLine.split(" ");
            if (parts.length < 2) {
                clientSocket.close();
                return;
            }

            String method = parts[0];
            String host;
            int port;

            if (method.equalsIgnoreCase("CONNECT")) {
                String[] hostPort = parts[1].split(":");
                host = hostPort[0];
                port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 443;

                remoteSocket = new Socket(host, port);
                remoteSocket.setSoTimeout(30000); // 30s timeout
                out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                out.flush();
            } else {
                host = pm.parseHost(header);
                if (host == null) {
                    clientSocket.close();
                    return;
                }
                port = 80;
                if (host.contains(":")) {
                    String[] hp = host.split(":");
                    host = hp[0];
                    port = Integer.parseInt(hp[1]);
                }
                remoteSocket = new Socket(host, port);
                remoteSocket.setSoTimeout(30000);
                remoteSocket.getOutputStream().write(buffer, 0, n);
            }

            pm.submitTask(new PThread(ip, in, remoteSocket.getOutputStream(), pm, clientSocket, remoteSocket));
            pm.submitTask(new PThread(ip, remoteSocket.getInputStream(), out, pm, clientSocket, remoteSocket));
        } catch (Exception e) {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            try {
                if (remoteSocket != null) remoteSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
