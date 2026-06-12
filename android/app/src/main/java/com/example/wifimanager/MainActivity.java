package com.example.wifimanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, HotspotManager.OnHotspotStateListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private HotspotManager hotspotManager;
    private HotspotRepository hotspotRepo;
    private DeviceAdapter deviceAdapter;
    private boolean scanActive = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService singleThreadPool;

    private TextView statusLabel;
    private Button toggleBtn;
    private Button proxyConfigBtn;
    private EditText ssidInput;
    private EditText passInput;
    private View proxyInfoLay;
    private ListView devicesListView;
    private ImageView qrCodeImg;

    public boolean isScanActive() { return scanActive; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusLabel = (TextView) findViewById(R.id.statusLabel);
        toggleBtn = (Button) findViewById(R.id.toggleHotspot);
        ssidInput = (EditText) findViewById(R.id.ssidInput);
        passInput = (EditText) findViewById(R.id.passwordInput);
        proxyInfoLay = findViewById(R.id.proxyLayout);
        proxyConfigBtn = (Button) findViewById(R.id.proxyBtn);
        devicesListView = (ListView) findViewById(R.id.devicesRecyclerView);
        qrCodeImg = (ImageView) findViewById(R.id.qrCodeImage);

        hotspotManager = new HotspotManager(this);
        hotspotRepo = new HotspotRepository(this);
        deviceAdapter = new DeviceAdapter(this, new ArrayList<Device>());
        devicesListView.setAdapter(deviceAdapter);

        toggleBtn.setOnClickListener(this);
        proxyConfigBtn.setOnClickListener(this);

        singleThreadPool = Executors.newSingleThreadExecutor();

        updateUI();
        startService(new Intent(this, UsageMonitorService.class));

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

        // Check GPS state before enabling hotspot
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
        String content = "WIFI:T:WPA;S:" + ssid + ";P:" + password + ";;";
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
        if (active) {
            if (!scanActive) {
                scanActive = true;
                mainHandler.post(new ScanRunnable(this, hotspotRepo, deviceAdapter, mainHandler));
            }
            // Also ensure QR code is generated if SSID/pass are available
            String s = ssidInput.getText().toString();
            String p = passInput.getText().toString();
            if (qrCodeImg.getVisibility() != View.VISIBLE && !s.isEmpty() && !p.isEmpty()) {
                generateQRCode(s, p);
            }
        } else if (!active) {
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
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, do nothing yet
        }
    }
}
