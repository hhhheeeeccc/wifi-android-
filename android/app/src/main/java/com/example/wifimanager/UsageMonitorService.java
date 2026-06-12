package com.example.wifimanager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.ProxyManager;
import java.util.List;

public class UsageMonitorService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private HotspotRepository repository;
    private ProxyManager proxyManager;
    private volatile boolean isRunning = false;
    private static final String CHANNEL_ID = "proxy_service_channel";
    private final IBinder binder = new LocalBinder(this);

    public boolean isRunning() { return isRunning; }

    public ProxyManager getProxyManager() { return proxyManager; }

    @Override public void onCreate() {
        super.onCreate();
        repository = new HotspotRepository(this);
        proxyManager = new ProxyManager(this);
        proxyManager.startProxy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Hotspot Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Proxy service is active")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
            startForeground(1, notification);
        }
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
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
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

    @Override public IBinder onBind(Intent intent) { return binder; }

    @Override public void onDestroy() {
        isRunning = false;
        if (proxyManager != null) {
            proxyManager.stopProxy();
        }
        super.onDestroy();
    }
}
