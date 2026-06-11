package com.example.wifimanager;
import android.os.Handler;
import com.example.wifimanager.model.Device;
import java.util.List;
public class MTask implements Runnable {
    private final UsageMonitorService s; private final Handler h = new Handler();
    public MTask(UsageMonitorService s) { this.s = s; }
    @Override public void run() {
        if (!s.run) return;
        List<Device> ds = s.repo.getConnectedDevices();
        for (Device d : ds) {
            long u = s.pm.getUse(d.getIpAddress());
            if (u > 0) {
                long total = d.getUsedData() + (u / (1024 * 1024));
                d.setUsedData(total);
                if (d.getDataLimit() > 0 && total >= d.getDataLimit()) d.setBlocked(true);
                s.repo.saveDevice(d);
            }
        }
        h.postDelayed(this, 10000);
    }
}
