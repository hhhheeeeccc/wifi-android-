package com.example.wifimanager;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
public class SpeedClickListener implements View.OnClickListener {
    private final Device d;
    private final HotspotManager hm;
    private final HotspotRepository r;
    private final DeviceAdapter a;
    public SpeedClickListener(Device d, HotspotManager hm, HotspotRepository r, DeviceAdapter a) {
        this.d = d;
        this.hm = hm;
        this.r = r;
        this.a = a;
    }
    @Override public void onClick(View v) {
        final EditText in = new EditText(v.getContext());
        in.setText(String.valueOf(d.getSpeedLimit()));
        new AlertDialog.Builder(v.getContext()).setTitle("Speed").setView(in).setPositiveButton("OK", new SpeedSaveListener(d, hm, r, in)).show();
    }
}
