import { useState, useEffect, useCallback } from 'react';
import { NativeModules, Alert, I18nManager, NativeEventEmitter, Platform } from 'react-native';
import { Device, HotspotState } from '../models/types';

const { WiFiModule } = NativeModules;
const WiFiEvents = new NativeEventEmitter(WiFiModule);

export const useMainViewModel = () => {
  const [state, setState] = useState<HotspotState>({
    isActive: false,
    ssid: 'MyWiFiHotspot',
    password: '12345678',
    isProxyEnabled: false,
  });

  const [devices, setDevices] = useState<Device[]>([]);

  useEffect(() => {
    I18nManager.forceRTL(true);

    const subscription = WiFiEvents.addListener('onDevicesUpdated', (newDevices: Device[]) => {
      setDevices(newDevices);
    });

    return () => {
      subscription.remove();
    };
  }, []);

  const toggleHotspot = useCallback(async () => {
    if (state.password && state.password.length < 8) {
      Alert.alert('خطأ', 'يجب أن تكون كلمة المرور 8 أحرف على الأقل');
      return;
    }

    try {
      const status = await WiFiModule.setHotspotEnabled(!state.isActive, state.ssid, state.password);

      if (status === 'IOS_MANUAL') {
        Alert.alert(
          'تنبيه',
          'نظام iOS لا يسمح بتفعيل البث برمجياً. يرجى تفعيله يدوياً من الإعدادات.',
          [
            { text: 'إلغاء', style: 'cancel' },
            { text: 'فتح الإعدادات', onPress: () => WiFiModule.openSettings() }
          ]
        );
        return;
      }

      if (status === 'MANUAL') {
        Alert.alert('تنبيه', 'يرجى تفعيل البث يدوياً من الإعدادات بسبب قيود النظام');
        WiFiModule.openSettings();
        return;
      }

      setState(prev => ({ ...prev, isActive: !prev.isActive }));
    } catch (error) {
      Alert.alert('خطأ', 'حدث خطأ أثناء محاولة تغيير حالة البث');
    }
  }, [state]);

  const toggleProxy = useCallback(() => {
    setState(prev => ({ ...prev, isProxyEnabled: !prev.isProxyEnabled }));
  }, []);

  const setSsid = (ssid: string) => setState(prev => ({ ...prev, ssid }));
  const setPassword = (password: string) => setState(prev => ({ ...prev, password }));

  const blockDevice = (deviceId: string) => {
    setDevices(prev => prev.map(d => d.id === deviceId ? { ...d, isBlocked: !d.isBlocked } : d));
  };

  return {
    state,
    devices,
    setSsid,
    setPassword,
    toggleHotspot,
    toggleProxy,
    blockDevice,
  };
};
