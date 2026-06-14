package com.example.wifimanager;

import android.os.Handler;
import android.widget.Toast;
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

    private static class FeedbackTask implements Runnable {
        private final MainActivity activity;
        private final boolean enable;
        private final int result;

        FeedbackTask(MainActivity activity, boolean enable, int result) {
            this.activity = activity;
            this.enable = enable;
            this.result = result;
        }

        @Override
        public void run() {
            if (enable) {
                if (result == 1 || result == 4) {
                    activity.updateUI();
                } else if (result == 2) {
                    activity.startLocalHotspotFlow();
                } else {
                    Toast.makeText(activity, activity.getString(R.string.hotspot_manual_instruction), Toast.LENGTH_SHORT).show();
                    activity.openSystemTethering();
                }
            } else {
                activity.updateUI();
            }
        }
    }

    @Override public void run() {
        final int res = hm.setHotspotEnabled(en, s, p);
        h.post(new FeedbackTask(a, en, res));
    }
}
