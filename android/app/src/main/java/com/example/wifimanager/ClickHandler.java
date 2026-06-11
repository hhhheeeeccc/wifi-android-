package com.example.wifimanager;
import android.view.View;
import com.example.wifimanager.model.Device;
public class ClickHandler implements View.OnClickListener {
    private final DeviceAdapter a; private final Device d;
    public ClickHandler(DeviceAdapter a, Device d) { this.a = a; this.d = d; }
    @Override public void onClick(View v) {
        d.setBlocked(!d.isBlocked());
        a.hm.blockDevice(d.getMacAddress(), d.isBlocked());
        a.repo.saveDevice(d);
        a.notifyDataSetChanged();
    }
}
