package com.example.wifimanager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import java.util.ArrayList;
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public HotspotManager hm; public HotspotRepository repo; public DeviceAdapter adp;
    public TextView lbl; public Button btn, pBtn; public EditText ssid, pass; public View lay; public ListView lv;
    public boolean scan = false; public Handler h = new Handler();
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main);
        lbl = (TextView) findViewById(R.id.statusLabel); btn = (Button) findViewById(R.id.toggleHotspot);
        ssid = (EditText) findViewById(R.id.ssidInput); pass = (EditText) findViewById(R.id.passwordInput);
        lay = findViewById(R.id.proxyLayout); pBtn = (Button) findViewById(R.id.proxyBtn);
        lv = (ListView) findViewById(R.id.devicesRecyclerView);
        hm = new HotspotManager(this); repo = new HotspotRepository(this);
        adp = new DeviceAdapter(this, new ArrayList()); lv.setAdapter(adp);
        btn.setOnClickListener(this); pBtn.setOnClickListener(this);
        updateUI(); startService(new Intent(this, UsageMonitorService.class));
    }
    @Override public void onClick(View v) {
        if (v.getId() == R.id.toggleHotspot) {
            String s = ssid.getText().toString(); String p = pass.getText().toString();
            if (p.length() < 8) return;
            boolean a = hm.isHotspotEnabled();
            if (hm.setHotspotEnabled(!a, s, p) > 0) updateUI();
        } else if (v.getId() == R.id.proxyBtn) {
            lay.setVisibility(lay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }
    public void updateUI() {
        boolean a = hm.isHotspotEnabled();
        lbl.setText(a ? "Active" : "Inactive"); btn.setText(a ? "Stop" : "Start");
        if (a && !scan) { scan = true; h.post(new STask(this)); } else if (!a) scan = false;
    }

    private void updateEmptyState(List<Device> devices) {
        if (devices.isEmpty()) {
            binding.emptyStateText.setVisibility(View.VISIBLE);
            binding.devicesRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyStateText.setVisibility(View.GONE);
            binding.devicesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
