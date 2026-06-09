package com.example.wifimanager.utils;

import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyManager {
    private static final int DEFAULT_PORT = 8080;
    private static final String TAG = "ProxyManager";
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private ExecutorService executorService;

    public synchronized void startProxy() {
        if (isRunning) return;
        isRunning = true;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                // Binding to a specific port to allow proxying for hotspot clients.
                serverSocket = new ServerSocket(DEFAULT_PORT);
                Log.d(TAG, "Proxy started on port " + DEFAULT_PORT);
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        if (isRunning) Log.e(TAG, "Error accepting connection", e);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Proxy server error", e);
            } finally {
                cleanup();
            }
        });
    }

    private void handleClient(Socket socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing client socket", e);
        }
    }

    public synchronized void stopProxy() {
        isRunning = false;
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        cleanup();
    }

    private void cleanup() {
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
