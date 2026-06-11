package com.example.wifimanager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import com.example.wifimanager.utils.ProxyManager;
import com.example.wifimanager.repository.HotspotRepository;
public class UsageMonitorService extends Service {
    public ProxyManager pm; public HotspotRepository repo; public boolean run = false;
    @Override public void onCreate() { super.onCreate(); repo = new HotspotRepository(this); pm = new ProxyManager(this); pm.startProxy(); }
    @Override public int onStartCommand(Intent i, int f, int s) { if (!run) { run = true; new MTask(this).run(); } return START_STICKY; }
    @Override public void onDestroy() { run = false; if (pm != null) pm.stopProxy(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
