package com.example.wifimanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.wifimanager.databinding.ActivityMainBinding;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private HotspotManager hotspotManager;
    private HotspotRepository repository;
    private DeviceAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;

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
            if (binding.proxyLayout.getVisibility() == android.view.View.VISIBLE) {
                binding.proxyLayout.setVisibility(android.view.View.GONE);
            } else {
                binding.proxyLayout.setVisibility(android.view.View.VISIBLE);
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
        isScanning = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isScanning) return;
                List<Device> devices = repository.getConnectedDevices();
                adapter.updateDevices(devices);
                handler.postDelayed(this, 5000);
            }
        });
    }

    private void stopDeviceScan() {
        isScanning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDeviceScan();
    }
}
