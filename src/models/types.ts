export interface Device {
  id: string;
  name: string;
  ip: string;
  mac: string;
  dataLimit?: number; // in MB
  usedData?: number; // in MB
  speedLimit?: number; // in KB/s
  isBlocked: boolean;
}

export interface HotspotState {
  isActive: boolean;
  ssid: string;
  password?: string;
  isProxyEnabled: boolean;
}
