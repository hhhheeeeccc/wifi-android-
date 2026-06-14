package com.example.wifimanager.utils;

import android.content.Intent;
import android.net.ProxyInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class HotspotVpnService extends VpnService {
    private static final String TAG = "HotspotVpnService";
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        String host = ProxyManager.IP_LOCAL_ONLY;
        int port = ProxyManager.DEFAULT_PROXY_PORT;

        if (intent != null) {
            host = intent.getStringExtra("proxy_host");
            if (host == null || host.isEmpty()) host = ProxyManager.IP_LOCAL_ONLY;
            port = intent.getIntExtra("proxy_port", ProxyManager.DEFAULT_PROXY_PORT);
        }

        startVpn(host, port);
        return START_STICKY;
    }

    private void startVpn(String host, int port) {
        if (vpnInterface != null) return;

        Builder builder = new Builder();
        builder.setSession("HotspotTunnel")
                .addAddress(ProxyManager.IP_VPN_VIRTUAL, 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(ProxyManager.DNS_GOOGLE);

        if (Build.VERSION.SDK_INT >= 29) {
            try {
                ProxyInfo proxyInfo = ProxyInfo.buildDirectProxy(host, port);
                builder.setHttpProxy(proxyInfo);
                Log.d(TAG, "VPN HTTP Proxy set to: " + host + ":" + port);
            } catch (Exception e) {
                Log.e(TAG, "Error setting VPN proxy", e);
            }
        }

        try {
            vpnInterface = builder.establish();
            Log.d(TAG, "VPN Interface established");
        } catch (Exception e) {
            Log.e(TAG, "Could not establish VPN interface", e);
        }
    }

    private void stopVpn() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
            vpnInterface = null;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        stopVpn();
        super.onRevoke();
    }
}
