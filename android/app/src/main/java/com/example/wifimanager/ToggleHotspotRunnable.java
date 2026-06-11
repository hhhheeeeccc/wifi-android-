package com.example.wifimanager;
import android.os.Handler;
import com.example.wifimanager.utils.HotspotManager;
public class ToggleHotspotRunnable implements Runnable {
    private final HotspotManager hm;
    private final boolean en;
    private final String s;
    private final String p;
    private final Handler h;
    private final MainActivity a;
    public ToggleHotspotRunnable(HotspotManager hm, boolean en, String s, String p, Handler h, MainActivity a) {
        this.hm = hm;
        this.en = en;
        this.s = s;
        this.p = p;
        this.h = h;
        this.a = a;
    }
    @Override public void run() {
        final int res = hm.setHotspotEnabled(en, s, p);
        if (res > 0) h.post(new UpdateUIRunnable(a));
    }
}
