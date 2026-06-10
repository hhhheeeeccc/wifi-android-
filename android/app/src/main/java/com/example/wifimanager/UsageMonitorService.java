package com.example.wifimanager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to monitor data usage of connected devices.
 * Optimization: Uses ScheduledExecutorService to perform periodic checks on a background thread,
 * avoiding main thread blocking for file I/O and root commands.
 */
public class UsageMonitorService extends Service {
    private static final String TAG = "UsageMonitorService";
    private HotspotRepository repository;
    private HotspotManager hotspotManager;
    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new HotspotRepository(this);
        hotspotManager = new HotspotManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            // Start monitoring periodically in the background
            scheduler.scheduleAtFixedRate(this::checkUsage, 0, 10, TimeUnit.SECONDS);
        }
        return START_STICKY;
    }

    private void checkUsage() {
        try {
            // repository.getConnectedDevices() performs file I/O on /proc/net/arp
            List<Device> devices = repository.getConnectedDevices();
            for (Device device : devices) {
                // In a real app, actual usage would be fetched from /proc/net/xt_qtaguid/stats
                if (device.getDataLimit() > 0) {
                    device.setUsedData(device.getUsedData() + 1); // Simulating usage
                    if (device.getUsedData() >= device.getDataLimit()) {
                        // hotspotManager.blockDevice() executes shell commands via su
                        hotspotManager.blockDevice(device.getMacAddress(), true);
                    }
                }
            }
        } catch (Exception e) {
            // Ensure the scheduler keeps running even if one iteration fails
            Log.e(TAG, "Error during usage check", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        super.onDestroy();
    }
}
