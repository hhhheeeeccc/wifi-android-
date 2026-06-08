package com.example.wifimanager.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.wifimanager.R;
import com.example.wifimanager.databinding.ActivityMainBinding;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceActionListener {
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private DeviceAdapter adapter;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        adapter = new DeviceAdapter(this);

        binding.devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.devicesRecyclerView.setAdapter(adapter);

        setupObservers();
        setupListeners();

        startScanning();
    }

    private void setupObservers() {
        viewModel.devices.observe(this, devices -> adapter.setDevices(devices));

        viewModel.isHotspotActive.observe(this, active -> {
            binding.statusLabel.setText(active ? getString(R.string.status_active) : getString(R.string.status_inactive));
            binding.toggleHotspot.setText(active ? getString(R.string.disable_hotspot) : getString(R.string.enable_hotspot));
        });

        viewModel.isProxyActive.observe(this, active -> {
            binding.proxyLayout.setVisibility(active ? View.VISIBLE : View.GONE);
        });

        viewModel.toastMessage.observe(this, message -> {
            if ("manual_instruction".equals(message)) {
                Toast.makeText(this, getString(R.string.hotspot_manual_instruction), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.toggleHotspot.setOnClickListener(v -> {
            String ssid = binding.ssidInput.getText().toString();
            String password = binding.passwordInput.getText().toString();
            if (password.length() > 0 && password.length() < 8) {
                Toast.makeText(this, getString(R.string.password_error), Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.toggleHotspot(ssid, password);
        });

        binding.proxyBtn.setOnClickListener(v -> viewModel.toggleProxy());
    }

    private void startScanning() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                viewModel.refreshDevices();
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    @Override
    public void onBlock(Device device) {
        viewModel.blockDevice(device);
    }

    @Override
    public void onLimit(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.limit_data));
        View view = getLayoutInflater().inflate(R.layout.dialog_limit, null);
        EditText input = view.findViewById(R.id.limitInput);
        builder.setView(view);
        builder.setPositiveButton("حفظ", (dialog, which) -> {
            String limitStr = input.getText().toString();
            if (!limitStr.isEmpty()) {
                viewModel.setDeviceDataLimit(device, Long.parseLong(limitStr));
            }
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    @Override
    public void onSpeedLimit(Device device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.speed_limit));
        View view = getLayoutInflater().inflate(R.layout.dialog_speed, null);
        EditText input = view.findViewById(R.id.speedInput);
        builder.setView(view);
        builder.setPositiveButton("حفظ", (dialog, which) -> {
            String speedStr = input.getText().toString();
            if (!speedStr.isEmpty()) {
                viewModel.setDeviceSpeedLimit(device, Integer.parseInt(speedStr));
            }
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
}
