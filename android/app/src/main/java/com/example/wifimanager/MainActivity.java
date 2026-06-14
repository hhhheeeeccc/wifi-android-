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

import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
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

    private TextView statusLabel;
    private EditText ssidInput, passInput;
    private Button toggleBtn, scanConnectBtn, proxyConfigBtn;
    private LinearLayout proxyInfoLay;
    private ListView devicesListView;
    private ImageView qrCodeImg;
    private TextView proxyHostTxt, proxyInstructionTxt;

    private HotspotManager hotspotManager;
    private WifiManager wm;
    private HotspotRepository hotspotRepo;
    private DeviceAdapter deviceAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService singleThreadPool;
    private boolean scanActive = false;

    private UsageMonitorService usageService;
    private boolean isBound = false;

    public boolean isScanActive() { return scanActive; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusLabel = (TextView) findViewById(R.id.statusLabel);
        ssidInput = (EditText) findViewById(R.id.ssidInput);
        passInput = (EditText) findViewById(R.id.passwordInput);
        toggleBtn = (Button) findViewById(R.id.toggleHotspot);
        scanConnectBtn = (Button) findViewById(R.id.scanConnectBtn);
        proxyConfigBtn = (Button) findViewById(R.id.proxyBtn);
        proxyInfoLay = (LinearLayout) findViewById(R.id.proxyLayout);
        devicesListView = (ListView) findViewById(R.id.devicesRecyclerView);
        qrCodeImg = (ImageView) findViewById(R.id.qrCodeImage);
        proxyHostTxt = (TextView) findViewById(R.id.proxy_host_txt);
        proxyInstructionTxt = (TextView) findViewById(R.id.proxy_instruction);

        hotspotManager = new HotspotManager(this);
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        hotspotRepo = new HotspotRepository(this);
        deviceAdapter = new DeviceAdapter(this, new ArrayList<Device>());
        devicesListView.setAdapter(deviceAdapter);

        toggleBtn.setOnClickListener(this);
        scanConnectBtn.setOnClickListener(this);
        proxyConfigBtn.setOnClickListener(this);

        singleThreadPool = Executors.newSingleThreadExecutor();

        Intent intent = new Intent(this, UsageMonitorService.class);
        startService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override public void onClick(View v) {
        if (v.getId() == R.id.toggleHotspot) {
            handleHotspotToggle();
        } else if (v.getId() == R.id.scanConnectBtn) {
            new IntentIntegrator(this).initiateScan();
        } else if (v.getId() == R.id.proxyBtn) {
            proxyInfoLay.setVisibility(proxyInfoLay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
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
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Could not open location settings", e);
            }
            return;
        }

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
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        }
    }

    @Override public void onStarted(String ssid, String password) {
        ssidInput.setText(ssid);
        passInput.setText(password);
        proxyInfoLay.setVisibility(View.VISIBLE);

        if (isBound && usageService != null && usageService.getProxyManager() != null) {
            usageService.getProxyManager().refreshHostIp();
        }

        generateQRCode(ssid, password);
        updateUI();
    }

    @Override public void onStopped() {
        qrCodeImg.setVisibility(View.GONE);
        updateUI();
    }

    @Override public void onFailure(int reason) {
        Toast.makeText(this, getString(R.string.hotspot_manual_instruction), Toast.LENGTH_SHORT).show();
        openSystemTethering();
    }

    private void generateQRCode(String ssid, String password) {
        String host = ProxyManager.IP_STANDARD_AP;
        if (isBound && usageService != null && usageService.getProxyManager() != null) {
            host = usageService.getProxyManager().getHostIp();
        }
        // Custom format: PH = Proxy Host, PP = Proxy Port
        String content = "WIFI:T:WPA;S:" + ssid + ";P:" + password + ";PH:" + host + ";PP:8080;;";
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCodeImg.setImageBitmap(bitmap);
            qrCodeImg.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error generating QR code", e);
        }
    }

    public void updateUI() {
        if (isFinishing()) return;
        boolean active = hotspotManager.isHotspotEnabled();
        statusLabel.setText(active ? R.string.status_active : R.string.status_inactive);
        toggleBtn.setText(active ? R.string.disable_hotspot : R.string.enable_hotspot);

        if (isBound && usageService != null) {
            ProxyManager pm = usageService.getProxyManager();
            if (pm != null) {
                if (active && !pm.isRunning()) {
                    pm.startProxy();
                }
                String host = pm.getHostIp();
                proxyHostTxt.setText(getString(R.string.proxy_host, host));
                proxyInstructionTxt.setText(getString(R.string.proxy_instruction, host));
            }
        }

        if (active) {
            if (!scanActive) {
                scanActive = true;
                mainHandler.post(new ScanRunnable(this, hotspotRepo, deviceAdapter, mainHandler));
            }
            String s = ssidInput.getText().toString();
            String p = passInput.getText().toString();
            if (qrCodeImg.getVisibility() != View.VISIBLE && !s.isEmpty() && !p.isEmpty()) {
                generateQRCode(s, p);
            }
        } else {
            scanActive = false;
        }
    }

    @Override protected void onDestroy() {
        scanActive = false;
        mainHandler.removeCallbacksAndMessages(null);
        if (singleThreadPool != null) {
            singleThreadPool.shutdownNow();
            singleThreadPool = null;
        }
        if (isBound) {
            unbindService(this);
            isBound = false;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                parseAndConnect(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void parseAndConnect(String data) {
        WifiQRParser.WifiData wifi = WifiQRParser.parse(data);
        if (wifi != null && !wifi.ssid.isEmpty() && !wifi.password.isEmpty()) {
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

            if (!ph.isEmpty()) {
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
        LocalBinder binder = (LocalBinder) service;
        usageService = (UsageMonitorService) binder.getService();
        isBound = true;
        updateUI();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
        usageService = null;
    }
}
