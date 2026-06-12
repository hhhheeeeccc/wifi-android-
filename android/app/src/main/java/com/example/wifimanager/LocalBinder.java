package com.example.wifimanager;
import android.os.Binder;
public class LocalBinder extends Binder {
    private final UsageMonitorService service;
    public LocalBinder(UsageMonitorService service) {
        this.service = service;
    }
    public UsageMonitorService getService() {
        return service;
    }
}
