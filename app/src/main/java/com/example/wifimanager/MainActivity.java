package com.example.wifimanager;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceActionListener {
    private HotspotManager hotspotManager;
    private ProxyManager proxyManager;
    private DeviceAdapter deviceAdapter;
    private List<Device> connectedDevices;
    private TextView statusLabel;
    private Button toggleButton, proxyBtn;
    private LinearLayout proxyLayout;
    private TextInputEditText ssidInput, passwordInput;
    private Handler handler = new Handler();
    private boolean isProxyEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hotspotManager = new HotspotManager(this);
        proxyManager = new ProxyManager();
        connectedDevices = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(connectedDevices, this);

        statusLabel = findViewById(R.id.statusLabel);
        toggleButton = findViewById(R.id.toggleHotspot);
        proxyBtn = findViewById(R.id.proxyBtn);
        proxyLayout = findViewById(R.id.proxyLayout);
        ssidInput = findViewById(R.id.ssidInput);
        passwordInput = findViewById(R.id.passwordInput);
        RecyclerView recyclerView = findViewById(R.id.devicesRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(deviceAdapter);

        updateUI();

        toggleButton.setOnClickListener(v -> {
            if (!hotspotManager.isHotspotEnabled()) {
                String ssid = ssidInput.getText().toString();
                String password = passwordInput.getText().toString();

                if (password.length() > 0 && password.length() < 8) {
                    Toast.makeText(this, getString(R.string.password_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (hotspotManager.setHotspotEnabled(true, ssid, password)) {
                    updateUI();
                } else {
                    Toast.makeText(this, getString(R.string.hotspot_manual_instruction), Toast.LENGTH_LONG).show();
                    hotspotManager.openHotspotSettings();
                }
            } else {
                hotspotManager.setHotspotEnabled(false, null, null);
                updateUI();
            }
        });

        proxyBtn.setOnClickListener(v -> {
            if (!isProxyEnabled) {
                proxyManager.startProxy();
                proxyLayout.setVisibility(View.VISIBLE);
                isProxyEnabled = true;
                Toast.makeText(this, "تم تفعيل وضع البروكسي"، Toast.LENGTH_SHORT).show();
            } else {
                proxyManager.stopProxy();
                proxyLayout.setVisibility(View.GONE);
                isProxyEnabled = false;
                Toast.makeText(this, "تم إيقاف وضع البروكسي"، Toast.LENGTH_SHORT).show();
            }
        });

        startDeviceScan();
    }

    private void updateUI() {
        boolean enabled = hotspotManager.isHotspotEnabled();
        statusLabel.setText(enabled ? getString(R.string.status_active) : getString(R.string.status_inactive));
        toggleButton.setText(enabled ? getString(R.string.disable_hotspot) : getString(R.string.enable_hotspot));
    }

    private void startDeviceScan() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanDevices();
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void scanDevices() {
        connectedDevices.clear();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted.length >= 4 && !splitted[0].equals("IP")) {
                    String ip = splitted[0];
                    String mac = splitted[3];
                    if (!mac.equals("00:00:00:00:00:00")) {
                        connectedDevices.add(new Device(ip, mac, "جهاز متصل (" + ip + ")"));
                    }
                }
            }
            br.close();
            deviceAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBlock(Device device) {
        device.setBlocked(!device.isBlocked());
        String msg = device.isBlocked() ? "تم حظر " : "تم إلغاء حظر ";
        Toast.makeText(this, msg + device.getIpAddress(), Toast.LENGTH_SHORT).show();
        deviceAdapter.notifyDataSetChanged();
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
                device.setDataLimit(Long.parseLong(limitStr));
                deviceAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());

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
                device.setSpeedLimit(Integer.parseInt(speedStr));
                Toast.makeText(this, "تم تحديد السرعة لـ " + speedStr + " كيلوبايت", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
