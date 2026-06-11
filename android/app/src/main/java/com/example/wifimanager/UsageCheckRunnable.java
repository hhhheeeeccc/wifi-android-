package com.example.wifimanager;
import android.os.Handler;
public class UsageCheckRunnable implements Runnable {
    private final UsageMonitorService s;
    private final Handler h;
    public UsageCheckRunnable(UsageMonitorService s, Handler h) { this.s = s; this.h = h; }
    @Override public void run() {
        if (!s.isRunning()) return;
        s.checkUsage();
        h.postDelayed(this, 10000);
    }
}
