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
    private final Handler h = new Handler(Looper.getMainLooper());
    public HotspotRepository r;
    public ProxyManager pm;
    public boolean run = false;
    public boolean isRunning() { return run; }
    @Override public void onCreate() {
        super.onCreate(); r = new HotspotRepository(this); pm = new ProxyManager(this); pm.startProxy();
    }
    @Override public int onStartCommand(Intent i, int f, int s) {
        if (!run) { run = true; h.post(new UsageCheckRunnable(this, h)); }
        return START_STICKY;
    }
    public void checkUsage() {
        List<Device> ds = r.getConnectedDevices();
        for (Device d : ds) {
            long u = pm.getAndResetUsage(d.getIpAddress());
            if (u > 0) {
                long total = d.getUsedData() + (u / (1024 * 1024));
                d.setUsedData(total);
                if (d.getDataLimit() > 0 && total >= d.getDataLimit()) d.setBlocked(true);
                r.saveDevice(d);
            }
        }
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() { run = false; if (pm != null) pm.stopProxy(); super.onDestroy(); }
}
