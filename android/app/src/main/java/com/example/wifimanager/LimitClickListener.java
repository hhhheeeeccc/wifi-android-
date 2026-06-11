package com.example.wifimanager;
import android.view.View;
import android.widget.EditText;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
public class LimitClickListener implements View.OnClickListener {
    private final Device d; private final HotspotRepository r; private final DeviceAdapter a;
    public LimitClickListener(Device d, HotspotRepository r, DeviceAdapter a) { this.d = d; this.r = r; this.a = a; }
    @Override public void onClick(View v) {
        final EditText in = new EditText(v.getContext()); in.setText(String.valueOf(d.getDataLimit()));
        new AlertDialog.Builder(v.getContext()).setTitle("Limit").setView(in).setPositiveButton("OK", new LimitSaveListener(d, r, a, in)).show();
    }
}
