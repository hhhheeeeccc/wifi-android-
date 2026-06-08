package com.example.wifimanager;

public class Device {
    private String ipAddress;
    private String macAddress;
    private String deviceName;
    private long dataLimit; // in MB
    private long usedData; // in MB
    private int speedLimit; // in KB/s
    private boolean isBlocked;

    public Device(String ipAddress, String macAddress, String deviceName) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.dataLimit = 0;
        this.usedData = 0;
        this.speedLimit = 0;
        this.isBlocked = false;
    }

    // Getters and Setters
    public String getIpAddress() { return ipAddress; }
    public String getMacAddress() { return macAddress; }
    public String getDeviceName() { return deviceName; }
    public long getDataLimit() { return dataLimit; }
    public void setDataLimit(long dataLimit) { this.dataLimit = dataLimit; }
    public long getUsedData() { return usedData; }
    public void setUsedData(long usedData) { this.usedData = usedData; }
    public int getSpeedLimit() { return speedLimit; }
    public void setSpeedLimit(int speedLimit) { this.speedLimit = speedLimit; }
    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}
