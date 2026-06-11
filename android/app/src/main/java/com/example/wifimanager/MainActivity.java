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
        updateEmptyState(new ArrayList<>());

        binding.toggleHotspot.setOnClickListener(v -> {
            String ssid = binding.ssidInput.getText().toString();
            String password = binding.passwordInput.getText().toString();

            if (password.length() < 8) {
                Toast.makeText(this, R.string.password_error, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean currentState = hotspotManager.isHotspotEnabled();
            boolean nextState = !currentState;

            // Show feedback
            Toast.makeText(this, nextState ? R.string.hotspot_starting : R.string.hotspot_stopping, Toast.LENGTH_SHORT).show();
            binding.toggleHotspot.setEnabled(false);

            Executors.newSingleThreadExecutor().execute(() -> {
                int result = hotspotManager.setHotspotEnabled(nextState, ssid, password);

                // Delay to allow system state to stabilize
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}

                uiHandler.post(() -> {
                    binding.toggleHotspot.setEnabled(true);
                    boolean realState = hotspotManager.isHotspotEnabled();
                    updateStatus(realState);

                    if (result > 0) {
                        if (!nextState && !realState) {
                            Toast.makeText(this, R.string.hotspot_stopped_success, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, R.string.toggle_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            });
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

    private void startDeviceScan() {
        if (scanExecutor == null || scanExecutor.isShutdown()) {
            scanExecutor = Executors.newSingleThreadScheduledExecutor();
            scanExecutor.scheduleAtFixedRate(() -> {
                List<Device> devices = repository.getConnectedDevices();
                uiHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        adapter.updateDevices(devices);
                        updateEmptyState(devices);
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
        updateStatus(hotspotManager.isHotspotEnabled());
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

    private void updateEmptyState(List<Device> devices) {
        if (devices.isEmpty()) {
            binding.emptyStateText.setVisibility(View.VISIBLE);
            binding.devicesRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyStateText.setVisibility(View.GONE);
            binding.devicesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
