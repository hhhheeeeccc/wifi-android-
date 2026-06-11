package com.example.wifimanager;
import android.content.DialogInterface;
import android.widget.EditText;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
public class LimitSaveListener implements DialogInterface.OnClickListener {
    private final Device d;
    private final HotspotRepository r;
    private final DeviceAdapter a;
    private final EditText in;
    public LimitSaveListener(Device d, HotspotRepository r, DeviceAdapter a, EditText in) {
        this.d = d;
        this.r = r;
        this.a = a;
        this.in = in;
    }
    @Override public void onClick(DialogInterface di, int w) {
        try {
            d.setDataLimit(Long.parseLong(in.getText().toString()));
            r.saveDevice(d);
            a.notifyDataSetChanged();
        } catch (Exception e) {
            // Ignore format error
        }
    }
}
