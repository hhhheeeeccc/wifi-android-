package com.example.wifimanager;
import android.view.View;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
public class BlockClickListener implements View.OnClickListener {
    private final Device d; private final HotspotManager hm; private final HotspotRepository r; private final DeviceAdapter a;
    public BlockClickListener(Device d, HotspotManager hm, HotspotRepository r, DeviceAdapter a) {
        this.d = d; this.hm = hm; this.r = r; this.a = a;
    }
    @Override public void onClick(View v) {
        d.setBlocked(!d.isBlocked());
        hm.blockDevice(d.getMacAddress(), d.isBlocked());
        r.saveDevice(d);
        a.notifyDataSetChanged();
    }
}
