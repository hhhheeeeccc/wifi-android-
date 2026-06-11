package com.example.wifimanager;
import android.content.DialogInterface;
import android.widget.EditText;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
public class SpeedSaveListener implements DialogInterface.OnClickListener {
    private final Device d;
    private final HotspotManager hm;
    private final HotspotRepository r;
    private final EditText in;
    public SpeedSaveListener(Device d, HotspotManager hm, HotspotRepository r, EditText in) {
        this.d = d;
        this.hm = hm;
        this.r = r;
        this.in = in;
    }
    @Override public void onClick(DialogInterface di, int w) {
        try {
            int k = Integer.parseInt(in.getText().toString());
            d.setSpeedLimit(k);
            hm.limitSpeed(d.getMacAddress(), k);
            r.saveDevice(d);
        } catch (NumberFormatException e) {
            // Ignore invalid speed format
        }
    }
}
