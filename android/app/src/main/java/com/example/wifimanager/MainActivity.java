package com.example.wifimanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.wifimanager.databinding.ActivityMainBinding;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private HotspotManager hotspotManager;
    private HotspotRepository repository;
    private DeviceAdapter adapter;
    private ScheduledExecutorService scanExecutor;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hotspotManager = new HotspotManager(this);
        repository = new HotspotRepository(this);

        setupUI();
        startService(new Intent(this, UsageMonitorService.class));
    }

    private void setupUI() {
        binding.devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(new ArrayList<>(), this);
        binding.devicesRecyclerView.setAdapter(adapter);

        binding.toggleHotspot.setOnClickListener(v -> {
            String ssid = binding.ssidInput.getText().toString();
            String password = binding.passwordInput.getText().toString();

            if (password.length() < 8) {
                Toast.makeText(this, R.string.password_error, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean currentState = hotspotManager.isHotspotEnabled();
            int result = hotspotManager.setHotspotEnabled(!currentState, ssid, password);

            if (result > 0) {
                updateStatus(!currentState);
            } else {
                Toast.makeText(this, "Failed to toggle hotspot", Toast.LENGTH_SHORT).show();
            }
        });

        binding.proxyBtn.setOnClickListener(v -> {
            if (binding.proxyLayout.getVisibility() == View.VISIBLE) {
                binding.proxyLayout.setVisibility(View.GONE);
            } else {
                binding.proxyLayout.setVisibility(View.VISIBLE);
            }
        });

        updateStatus(hotspotManager.isHotspotEnabled());
    }

    private void updateStatus(boolean active) {
        binding.statusLabel.setText(active ? R.string.status_active : R.string.status_inactive);
        binding.toggleHotspot.setText(active ? R.string.disable_hotspot : R.string.enable_hotspot);
        if (active) startDeviceScan();
        else stopDeviceScan();
    }

    /**
     * Periodic device scanning.
     * Optimization: Offloads ARP table parsing to a background thread to prevent UI stuttering.
     */
    private void startDeviceScan() {
        if (scanExecutor == null || scanExecutor.isShutdown()) {
            scanExecutor = Executors.newSingleThreadScheduledExecutor();
            scanExecutor.scheduleAtFixedRate(() -> {
                // Background thread: I/O operation
                List<Device> devices = repository.getConnectedDevices();
                // Post result to UI thread
                uiHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        adapter.updateDevices(devices);
                    }
                });
            }, 0, 5, TimeUnit.SECONDS);
        }
    }

    private void stopDeviceScan() {
        if (scanExecutor != null) {
            scanExecutor.shutdown();
            scanExecutor = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hotspotManager.isHotspotEnabled()) {
            startDeviceScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDeviceScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDeviceScan();
    }
}
