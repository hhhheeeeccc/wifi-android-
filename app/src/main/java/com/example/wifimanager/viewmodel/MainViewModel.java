package com.example.wifimanager.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final HotspotRepository repository;
    private final MutableLiveData<List<Device>> _devices = new MutableLiveData<>();
    public final LiveData<List<Device>> devices = _devices;

    private final MutableLiveData<Boolean> _isHotspotActive = new MutableLiveData<>();
    public final LiveData<Boolean> isHotspotActive = _isHotspotActive;

    private final MutableLiveData<Boolean> _isProxyActive = new MutableLiveData<>();
    public final LiveData<Boolean> isProxyActive = _isProxyActive;

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public final LiveData<String> toastMessage = _toastMessage;

    public MainViewModel(Application application) {
        super(application);
        this.repository = new HotspotRepository(application);
        _isHotspotActive.setValue(repository.isHotspotActive());
        _isProxyActive.setValue(false);
    }

    public void toggleHotspot(String ssid, String password) {
        boolean currentState = _isHotspotActive.getValue() != null && _isHotspotActive.getValue();
        if (repository.toggleHotspot(!currentState, ssid, password)) {
            _isHotspotActive.setValue(!currentState);
        } else {
            if (!currentState) {
                _toastMessage.setValue("manual_instruction");
                repository.openSettings();
            }
        }
    }

    public void toggleProxy() {
        boolean currentState = _isProxyActive.getValue() != null && _isProxyActive.getValue();
        repository.toggleProxy(!currentState);
        _isProxyActive.setValue(!currentState);
    }

    public void refreshDevices() {
        _devices.postValue(repository.getConnectedDevices());
    }

    public void blockDevice(Device device) {
        device.setBlocked(!device.isBlocked());
        refreshDevices();
    }

    public void setDeviceDataLimit(Device device, long limit) {
        device.setDataLimit(limit);
        refreshDevices();
    }

    public void setDeviceSpeedLimit(Device device, int speed) {
        device.setSpeedLimit(speed);
        refreshDevices();
    }
}
