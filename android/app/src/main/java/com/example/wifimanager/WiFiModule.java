package com.example.wifimanager;

import android.os.Handler;
import android.os.Looper;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.example.wifimanager.utils.HotspotManager;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;

import java.util.List;

public class WiFiModule extends ReactContextBaseJavaModule {
    private final HotspotManager hotspotManager;
    private final HotspotRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;

    public WiFiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.hotspotManager = new HotspotManager(reactContext);
        this.repository = new HotspotRepository(reactContext);
    }

    @Override
    public String getName() {
        return "WiFiModule";
    }

    @ReactMethod
    public void setHotspotEnabled(boolean enabled, String ssid, String password, Promise promise) {
        int result = hotspotManager.setHotspotEnabled(enabled, ssid, password);

        String status;
        switch (result) {
            case 1: status = "ROOT_OK"; break;
            case 2: status = "REFLECTION_OK"; break;
            case 3: status = "LOCAL_ONLY"; break;
            case 4: status = "STOPPED"; break;
            case 5: status = "LEGACY_OK"; break;
            default: status = "FAILED"; break;
        }

        if (!status.equals("FAILED")) {
            if (enabled) startDeviceScan();
            else stopDeviceScan();
            promise.resolve(status);
        } else {
            promise.reject("ERR_HOTSPOT", "Failed to toggle hotspot");
        }
    }

    @ReactMethod
    public void openSettings() {
        hotspotManager.openHotspotSettings();
    }

    private void startDeviceScan() {
        isScanning = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isScanning) return;
                sendDeviceUpdate();
                handler.postDelayed(this, 5000);
            }
        });
    }

    private void stopDeviceScan() {
        isScanning = false;
    }

    private void sendDeviceUpdate() {
        List<Device> devices = repository.getConnectedDevices();
        WritableArray array = Arguments.createArray();
        for (Device d : devices) {
            WritableMap map = Arguments.createMap();
            map.putString("id", d.getMacAddress());
            map.putString("name", d.getDeviceName());
            map.putString("ip", d.getIpAddress());
            map.putString("mac", d.getMacAddress());
            map.putBoolean("isBlocked", d.isBlocked());
            array.pushMap(map);
        }
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("onDevicesUpdated", array);
    }
}
