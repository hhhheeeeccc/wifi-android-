package com.example.wifimanager;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import java.util.List;

public class UsageMonitorService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private HotspotRepository repository;
    private HotspotManager hotspotManager;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new HotspotRepository(this);
        hotspotManager = new HotspotManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startMonitoring();
        }
        return START_STICKY;
    }

    private void startMonitoring() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                checkUsage();
                handler.postDelayed(this, 10000); // Check every 10 seconds
            }
        });
    }

    private void checkUsage() {
        List<Device> devices = repository.getConnectedDevices();
        for (Device device : devices) {
            // In a real app, you would fetch actual usage from /proc/net/xt_qtaguid/stats
            // For now, we simulate data usage increment
            if (device.getDataLimit() > 0) {
                device.setUsedData(device.getUsedData() + 1); // Simulating 1MB used
                if (device.getUsedData() >= device.getDataLimit()) {
                    hotspotManager.blockDevice(device.getMacAddress(), true);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }
}
