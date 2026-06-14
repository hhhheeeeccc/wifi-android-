package com.example.wifimanager;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.ProxyInfo;
import android.net.VpnService;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import com.example.wifimanager.utils.HotspotVpnService;
import com.example.wifimanager.utils.ProxyManager;
import com.example.wifimanager.utils.WifiQRParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, HotspotManager.OnHotspotStateListener, ServiceConnection {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int VPN_REQUEST_CODE = 200;

    private TextView statusLabel, proxyHostTxt, proxyPortTxt, proxyInstructionTxt;
    private EditText ssidInput, passInput;
    private Button toggleBtn, scanBtn, proxyToggleBtn, vpnBtn;
    private LinearLayout proxyInfoLay;
    private ImageView qrCodeImg;
    private ListView deviceListView;

    private HotspotManager hotspotManager;
    private HotspotRepository hotspotRepo;
    private DeviceAdapter deviceAdapter;
    private UsageMonitorService usageService;
    private boolean isBound = false;
    private Handler mainHandler;
    private ExecutorService singleThreadPool;
    private boolean scanActive = false;

    private String lastProxyHost = "";
    private int lastProxyPort = ProxyManager.DEFAULT_PROXY_PORT;
    private WifiManager wm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught Exception", throwable);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, "حدث خطأ: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
            });
        });

        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            Log.e(TAG, "Error in setContentView", e);
            return;
        }

        mainHandler = new Handler(Looper.getMainLooper());
        singleThreadPool = Executors.newSingleThreadExecutor();

        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        hotspotManager = new HotspotManager(this);
        hotspotRepo = new HotspotRepository(this);
        deviceAdapter = new DeviceAdapter(this, new ArrayList<>());

        try {
            initViews();
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews", e);
        }

        mainHandler.postDelayed(this::setupService, 1000);
        checkAndRequestPermissions();
    }

    private void initViews() {
        statusLabel = findViewById(R.id.statusLabel);
        ssidInput = findViewById(R.id.ssidInput);
        passInput = findViewById(R.id.passwordInput);
        toggleBtn = findViewById(R.id.toggleHotspot);
        scanBtn = findViewById(R.id.scanConnectBtn);
        vpnBtn = findViewById(R.id.toggleVpnBtn);
        proxyToggleBtn = findViewById(R.id.proxyBtn);
        proxyInfoLay = findViewById(R.id.proxyLayout);
        proxyHostTxt = findViewById(R.id.proxy_host_txt);
        proxyPortTxt = findViewById(R.id.proxy_port_txt);
        proxyInstructionTxt = findViewById(R.id.proxy_instruction);
        qrCodeImg = findViewById(R.id.qrCodeImage);
        deviceListView = findViewById(R.id.devicesRecyclerView);

        if (deviceListView != null) deviceListView.setAdapter(deviceAdapter);
        if (toggleBtn != null) toggleBtn.setOnClickListener(this);
        if (scanBtn != null) scanBtn.setOnClickListener(this);
        if (vpnBtn != null) vpnBtn.setOnClickListener(this);
        if (proxyToggleBtn != null) proxyToggleBtn.setOnClickListener(this);
    }

    private void setupService() {
        try {
            Intent intent = new Intent(this, UsageMonitorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            bindService(intent, this, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }

    public boolean isScanActive() { return scanActive; }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.toggleHotspot) {
            handleHotspotToggle();
        } else if (id == R.id.scanConnectBtn) {
            try {
                new IntentIntegrator(this).initiateScan();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في بدء الماسح", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.toggleVpnBtn) {
            handleVpnToggle();
        } else if (id == R.id.proxyBtn) {
            if (proxyInfoLay != null) {
                proxyInfoLay.setVisibility(proxyInfoLay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void handleVpnToggle() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            startVpnService();
        }
    }

    private void startVpnService() {
        try {
            Intent intent = new Intent(this, HotspotVpnService.class);
            intent.putExtra("proxy_host", lastProxyHost.isEmpty() ? ProxyManager.IP_LOCAL_ONLY : lastProxyHost);
            intent.putExtra("proxy_port", lastProxyPort);
            startService(intent);
            Toast.makeText(this, R.string.vpn_started, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "VPN Start Error", e);
        }
    }

    private boolean isGPSEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void handleHotspotToggle() {
        final boolean enable = !hotspotManager.isHotspotEnabled();
        if (!enable) {
            hotspotManager.stopLocalOnlyHotspot();
            if (singleThreadPool != null && !singleThreadPool.isShutdown()) {
                singleThreadPool.execute(new ToggleHotspotRunnable(hotspotManager, false, "", "", mainHandler, this));
            }
            return;
        }

        if (!isGPSEnabled()) {
            Toast.makeText(this, R.string.gps_required, Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } catch (Exception e) {
                Log.e(TAG, "Could not open location settings", e);
            }
            return;
        }

        if (ssidInput == null || passInput == null) return;
        final String ssid = ssidInput.getText().toString();
        final String pass = passInput.getText().toString();
        if (pass.length() < 8) {
            Toast.makeText(this, R.string.password_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (singleThreadPool != null && !singleThreadPool.isShutdown()) {
            singleThreadPool.execute(new ToggleHotspotRunnable(hotspotManager, true, ssid, pass, mainHandler, this));
        }
    }

    public void startLocalHotspotFlow() {
        if (Build.VERSION.SDK_INT >= 26) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                return;
            }
            Toast.makeText(this, getString(R.string.starting_local_hotspot), Toast.LENGTH_SHORT).show();
            hotspotManager.startLocalOnlyHotspot(this);
        } else {
            openSystemTethering();
        }
    }

    public void openSystemTethering() {
        Toast.makeText(this, R.string.hotspot_manual_instruction, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        try {
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception ignored) {
                Log.e(TAG, "Could not open settings");
            }
        }
    }

    @Override public void onStarted(String ssid, String password) {
        if (ssidInput != null) ssidInput.setText(ssid);
        if (passInput != null) passInput.setText(password);
        if (proxyInfoLay != null) proxyInfoLay.setVisibility(View.VISIBLE);

        if (isBound && usageService != null && usageService.getProxyManager() != null) {
            usageService.getProxyManager().refreshHostIp();
        }

        generateQRCode(ssid, password);
        updateUI();
    }

    @Override public void onStopped() {
        if (qrCodeImg != null) qrCodeImg.setVisibility(View.GONE);
        updateUI();
    }

    @Override public void onFailure(int reason) {
        Toast.makeText(this, getString(R.string.hotspot_manual_instruction), Toast.LENGTH_SHORT).show();
        openSystemTethering();
    }

    private void generateQRCode(String ssid, String password) {
        if (ssid == null || password == null) return;
        String host = ProxyManager.IP_STANDARD_AP;
        if (isBound && usageService != null && usageService.getProxyManager() != null) {
            host = usageService.getProxyManager().getHostIp();
        }
        String content = "WIFI:T:WPA;S:" + ssid + ";P:" + password + ";PH:" + host + ";PP:8080;;";
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            if (qrCodeImg != null) {
                qrCodeImg.setImageBitmap(bitmap);
                qrCodeImg.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating QR code", e);
        }
    }

    public void updateUI() {
        if (isFinishing()) return;
        boolean active = hotspotManager.isHotspotEnabled();
        if (statusLabel != null) statusLabel.setText(active ? R.string.status_active : R.string.status_inactive);
        if (toggleBtn != null) toggleBtn.setText(active ? R.string.disable_hotspot : R.string.enable_hotspot);

        if (isBound && usageService != null) {
            ProxyManager pm = usageService.getProxyManager();
            if (pm != null) {
                if (active && !pm.isRunning()) {
                    pm.startProxy();
                }
                String host = pm.getHostIp();
                if (proxyHostTxt != null) proxyHostTxt.setText(getString(R.string.proxy_host, host));
                if (proxyInstructionTxt != null) proxyInstructionTxt.setText(getString(R.string.proxy_instruction, host));
            }
        }

        if (active) {
            if (!scanActive) {
                scanActive = true;
                mainHandler.post(new ScanRunnable(this, hotspotRepo, deviceAdapter, mainHandler));
            }
            if (ssidInput != null && passInput != null && qrCodeImg != null) {
                String s = ssidInput.getText().toString();
                String p = passInput.getText().toString();
                if (qrCodeImg.getVisibility() != View.VISIBLE && !s.isEmpty() && !p.isEmpty()) {
                    generateQRCode(s, p);
                }
            }
        } else {
            scanActive = false;
        }
    }

    @Override protected void onDestroy() {
        scanActive = false;
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        if (singleThreadPool != null) {
            singleThreadPool.shutdownNow();
            singleThreadPool = null;
        }
        if (isBound) {
            try {
                unbindService(this);
            } catch (Exception ignored) {
                Log.e(TAG, "Error unbinding service");
            }
            isBound = false;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService();
        } else {
            try {
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (result != null && result.getContents() != null) {
                    parseAndConnect(result.getContents());
                } else {
                    super.onActivityResult(requestCode, resultCode, data);
                }
            } catch (Exception e) {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void parseAndConnect(String data) {
        WifiQRParser.WifiData wifi = WifiQRParser.parse(data);
        if (wifi != null && !wifi.ssid.isEmpty() && !wifi.password.isEmpty()) {
            this.lastProxyHost = wifi.proxyHost;
            this.lastProxyPort = wifi.proxyPort;
            connectToWifiWithProxy(wifi.ssid, wifi.password, wifi.proxyHost, wifi.proxyPort);
        } else {
            Toast.makeText(this, R.string.invalid_qr, Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToWifiWithProxy(String ssid, String pass, String ph, int pp) {
        if (Build.VERSION.SDK_INT >= 29) {
            Toast.makeText(this, R.string.connecting_wifi, Toast.LENGTH_SHORT).show();
            WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(pass);

            if (ph != null && !ph.isEmpty()) {
                try {
                    ProxyInfo proxy = ProxyInfo.buildDirectProxy(ph, pp);
                    Method setProxyMethod = builder.getClass().getMethod("setHttpProxy", ProxyInfo.class);
                    setProxyMethod.invoke(builder, proxy);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting proxy suggestion", e);
                }
            }

            WifiNetworkSuggestion suggestion = builder.build();
            int status = wm.addNetworkSuggestions(Collections.singletonList(suggestion));
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Toast.makeText(this, R.string.network_added_success, Toast.LENGTH_SHORT).show();
                if (ph != null && !ph.isEmpty()) {
                    handleVpnToggle();
                }
            } else {
                Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "هذه الخاصية تتطلب أندرويد 10 فما فوق", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        try {
            LocalBinder binder = (LocalBinder) service;
            usageService = (UsageMonitorService) binder.getService();
            isBound = true;
            updateUI();
        } catch (Exception e) {
            Log.e(TAG, "Service Connect Error", e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
        usageService = null;
    }
}
