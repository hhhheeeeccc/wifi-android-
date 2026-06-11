package com.example.wifimanager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.wifimanager.model.Device;
import com.example.wifimanager.repository.HotspotRepository;
import com.example.wifimanager.utils.HotspotManager;
import java.util.List;
public class DeviceAdapter extends BaseAdapter {
    private final Context ctx; private List<Device> list; private final HotspotManager hm; private final HotspotRepository repo;
    public DeviceAdapter(Context c, List<Device> l) { this.ctx = c; this.list = l; this.hm = new HotspotManager(c); this.repo = new HotspotRepository(c); }
    public void update(List<Device> nl) { this.list = nl; notifyDataSetChanged(); }
    @Override public int getCount() { return list != null ? list.size() : 0; }
    @Override public Object getItem(int p) { return list.get(p); }
    @Override public long getItemId(int p) { return p; }
    @Override public View getView(int p, View v, ViewGroup pr) {
        if (v == null) v = LayoutInflater.from(ctx).inflate(R.layout.device_item, pr, false);
        Device d = list.get(p);
        ((TextView) v.findViewById(R.id.deviceName)).setText(d.getDeviceName());
        ((TextView) v.findViewById(R.id.deviceDetails)).setText(d.getIpAddress());
        Button b = (Button) v.findViewById(R.id.btnBlock); b.setText(d.isBlocked() ? "Unblock" : "Block");
        b.setOnClickListener(new BlockClickListener(d, hm, repo, this));
        v.findViewById(R.id.btnLimit).setOnClickListener(new LimitClickListener(d, repo, this));
        v.findViewById(R.id.btnSpeed).setOnClickListener(new SpeedClickListener(d, hm, repo, this));
        ProgressBar pg = (ProgressBar) v.findViewById(R.id.dataProgress);
        if (d.getDataLimit() > 0) { pg.setVisibility(View.VISIBLE); pg.setProgress((int)Math.min(100, (d.getUsedData()*100L)/d.getDataLimit())); }
        else pg.setVisibility(View.GONE);
        return v;
    }
}
