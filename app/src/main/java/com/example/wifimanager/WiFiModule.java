package com.example.wifimanager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.example.wifimanager.utils.HotspotManager;

public class WiFiModule extends ReactContextBaseJavaModule {
    private final HotspotManager hotspotManager;

    public WiFiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.hotspotManager = new HotspotManager(reactContext);
    }

    @Override
    public String getName() {
        return "WiFiModule";
    }

    @ReactMethod
    public void setHotspotEnabled(boolean enabled, String ssid, String password, Promise promise) {
        if (hotspotManager.setHotspotEnabled(enabled, ssid, password)) {
            promise.resolve(true);
        } else {
            promise.reject("ERR_HOTSPOT", "Failed to toggle hotspot");
        }
    }

    @ReactMethod
    public void openSettings() {
        hotspotManager.openHotspotSettings();
    }
}
