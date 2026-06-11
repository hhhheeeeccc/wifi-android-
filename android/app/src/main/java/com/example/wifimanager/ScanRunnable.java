package com.example.wifimanager;
import android.os.Handler;
import com.example.wifimanager.repository.HotspotRepository;
public class ScanRunnable implements Runnable {
    private final MainActivity a;
    private final HotspotRepository r;
    private final DeviceAdapter ad;
    private final Handler h;
    public ScanRunnable(MainActivity a, HotspotRepository r, DeviceAdapter ad, Handler h) {
        this.a = a; this.r = r; this.ad = ad; this.h = h;
    }
    @Override public void run() {
        if (!a.scanActive) return;
        ad.update(r.getConnectedDevices());
        h.postDelayed(this, 5000);
    }
}
