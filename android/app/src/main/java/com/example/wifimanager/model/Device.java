package com.example.wifimanager.model;

public class Device {
    private final String ipAddress;
    private final String macAddress;
    private String deviceName;
    private long dataLimit;
    private long usedData;
    private int speedLimit;
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
    public void setBlocked(boolean blocked) { this.isBlocked = blocked; }
}
