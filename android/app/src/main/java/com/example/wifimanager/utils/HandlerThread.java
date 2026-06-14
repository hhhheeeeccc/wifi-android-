package com.example.wifimanager.utils;
import com.example.wifimanager.repository.HotspotRepository;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class HandlerThread implements Runnable {
    private final ProxyManager pm;
    private final Socket clientSocket;
    private static final int BUFFER_SIZE = 65536;

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
            // Optimize client socket
            clientSocket.setTcpNoDelay(true);
            clientSocket.setSendBufferSize(BUFFER_SIZE);
            clientSocket.setReceiveBufferSize(BUFFER_SIZE);

            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int n = in.read(buffer);
            if (n <= 0) {
                clientSocket.close();
                return;
            }

            String header = new String(buffer, 0, n);
            String[] headerLines = header.split("\r\n");
            if (headerLines.length == 0) {
                clientSocket.close();
                return;
            }

            String firstLine = headerLines[0];
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

                remoteSocket = new Socket();
                configureRemoteSocket(remoteSocket);
                remoteSocket.connect(new InetSocketAddress(host, port), 10000);

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
                remoteSocket = new Socket();
                configureRemoteSocket(remoteSocket);
                remoteSocket.connect(new InetSocketAddress(host, port), 10000);
                remoteSocket.getOutputStream().write(buffer, 0, n);
                remoteSocket.getOutputStream().flush();
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

    private void configureRemoteSocket(Socket s) throws IOException {
        s.setTcpNoDelay(true);
        s.setSendBufferSize(BUFFER_SIZE);
        s.setReceiveBufferSize(BUFFER_SIZE);
        s.setSoTimeout(30000);
    }
}
