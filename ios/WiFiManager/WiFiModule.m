#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(WiFiModule, RCTEventEmitter)

RCT_EXTERN_METHOD(setHotspotEnabled:(BOOL)enabled ssid:(NSString *)ssid password:(NSString *)password resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(openSettings)

@end
