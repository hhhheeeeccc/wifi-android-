package com.example.wifimanager;
public class UpdateUIRunnable implements Runnable {
    private final MainActivity a;
    public UpdateUIRunnable(MainActivity a) { this.a = a; }
    @Override public void run() { a.updateUI(); }
}
