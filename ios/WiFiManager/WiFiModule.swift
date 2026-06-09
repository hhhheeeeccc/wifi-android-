import Foundation
import React

@objc(WiFiModule)
class WiFiModule: RCTEventEmitter {

  override func supportedEvents() -> [String]! {
    return ["onDevicesUpdated"]
  }

  @objc(setHotspotEnabled:ssid:password:resolver:rejecter:)
  func setHotspotEnabled(enabled: Bool, ssid: String, password: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    // iOS does not allow programmatically enabling Personal Hotspot for 3rd party apps.
    // We will return a specific status to indicate manual action is required.
    if enabled {
      resolve("IOS_MANUAL")
    } else {
      resolve("STOPPED")
    }
  }

  @objc
  func openSettings() {
    if let url = URL(string: "App-Prefs:root=INTERNET_TETHERING") {
      if UIApplication.shared.canOpenURL(url) {
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
      } else if let generalUrl = URL(string: UIApplication.openSettingsURLString) {
        UIApplication.shared.open(generalUrl, options: [:], completionHandler: nil)
      }
    }
  }

  override static func requiresMainQueueSetup() -> Bool {
    return true
  }
}
