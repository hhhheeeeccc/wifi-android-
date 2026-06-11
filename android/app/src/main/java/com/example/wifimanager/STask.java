package com.example.wifimanager;
public class STask implements Runnable {
    private final MainActivity a;
    public STask(MainActivity a) { this.a = a; }
    @Override public void run() {
        if (!a.scan) return;
        a.adp.update(a.repo.getConnectedDevices());
        a.h.postDelayed(this, 5000);
    }
}
