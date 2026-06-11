package com.example.wifimanager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, HotspotManager.OnHotspotStateListener {
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

    private void handleHotspotToggle() {
        final boolean enable = !hotspotManager.isHotspotEnabled();
        if (!enable) {
            hotspotManager.stopLocalOnlyHotspot();
            if (singleThreadPool != null && !singleThreadPool.isShutdown()) {
                singleThreadPool.execute(new ToggleHotspotRunnable(hotspotManager, false, "", "", mainHandler, this));
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
        updateUI();
    }

    @Override public void onStopped() {
        updateUI();
    }

    @Override public void onFailure(int reason) {
        Toast.makeText(this, getString(R.string.hotspot_manual_instruction), Toast.LENGTH_SHORT).show();
        openSystemTethering();
    }

    public void updateUI() {
        if (isFinishing()) return;
        boolean active = hotspotManager.isHotspotEnabled();
        statusLabel.setText(active ? R.string.status_active : R.string.status_inactive);
        toggleBtn.setText(active ? R.string.disable_hotspot : R.string.enable_hotspot);
        if (active && !scanActive) {
            scanActive = true;
            mainHandler.post(new ScanRunnable(this, hotspotRepo, deviceAdapter, mainHandler));
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
