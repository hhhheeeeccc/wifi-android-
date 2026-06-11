package com.example.wifimanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private HotspotManager hotspotManager;
    private HotspotRepository hotspotRepo;
    private DeviceAdapter deviceAdapter;
    public boolean scanActive = false;
    public final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService singleThreadPool;

    private TextView statusLabel;
    private Button toggleBtn, proxyConfigBtn;
    private EditText ssidInput, passInput;
    private View proxyInfoLay;
    private ListView devicesListView;

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
    }

    @Override public void onClick(View v) {
        if (v.getId() == R.id.toggleHotspot) {
            final String ssid = ssidInput.getText().toString();
            final String pass = passInput.getText().toString();
            if (pass.length() < 8) {
                Toast.makeText(this, R.string.password_error, Toast.LENGTH_SHORT).show();
                return;
            }
            if (singleThreadPool != null && !singleThreadPool.isShutdown()) {
                singleThreadPool.execute(new ToggleHotspotRunnable(hotspotManager, !hotspotManager.isHotspotEnabled(), ssid, pass, mainHandler, this));
            }
        } else if (v.getId() == R.id.proxyBtn) {
            proxyInfoLay.setVisibility(proxyInfoLay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
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
}
