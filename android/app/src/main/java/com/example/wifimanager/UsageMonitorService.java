package com.example.wifimanager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.ProxyManager;
import java.util.List;
public class UsageMonitorService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private HotspotRepository repository;
    private ProxyManager proxyManager;
    private volatile boolean isRunning = false;

    public boolean isRunning() { return isRunning; }

    @Override public void onCreate() {
        super.onCreate();
        repository = new HotspotRepository(this);
        proxyManager = new ProxyManager(this);
        proxyManager.startProxy();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            handler.post(new UsageCheckRunnable(this, handler));
        }
        return START_STICKY;
    }

    public void checkUsage() {
        List<Device> devices = repository.getConnectedDevices();
        for (Device device : devices) {
            long usageBytes = proxyManager.getAndResetUsage(device.getIpAddress());
            if (usageBytes > 0) {
                long currentBytes = device.getUsedData() * 1024 * 1024;
                long totalBytes = currentBytes + usageBytes;
                long totalMb = totalBytes / (1024 * 1024);
                device.setUsedData(totalMb);
                if (device.getDataLimit() > 0 && totalMb >= device.getDataLimit()) {
                    device.setBlocked(true);
                }
                repository.saveDevice(device);
            }
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        isRunning = false;
        if (proxyManager != null) {
            proxyManager.stopProxy();
        }
        super.onDestroy();
    }
}
