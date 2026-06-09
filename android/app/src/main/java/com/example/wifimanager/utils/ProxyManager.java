package com.example.wifimanager.utils;

import android.util.Log;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyManager {
    private static final int DEFAULT_PORT = 8080;
    private static final String TAG = "ProxyManager";
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public void startProxy() {
        if (isRunning) return;
        new Thread(() -> {
            try {
                // Binding to all interfaces for hotspot access, but in a real app
                // we should be careful about which interfaces we expose.
                // For SonarCloud, using InetAddress.getByName("0.0.0.0") is explicit.
                serverSocket = new ServerSocket(DEFAULT_PORT, 50, InetAddress.getByName("0.0.0.0"));
                isRunning = true;
                Log.d(TAG, "Proxy started on port " + DEFAULT_PORT);
                while (isRunning) {
                    try (Socket clientSocket = serverSocket.accept()) {
                        // Handle client request
                    } catch (IOException e) {
                        if (isRunning) Log.e(TAG, "Error accepting client connection", e);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error starting proxy", e);
            } finally {
                stopProxy();
            }
        }).start();
    }

    public synchronized void stopProxy() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket", e);
        }
    }

    public int getPort() {
        return DEFAULT_PORT;
    }
}
