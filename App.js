import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
  FlatList,
  I18nManager,
  NativeModules,
  Alert
} from 'react-native';

// Force RTL for Arabic
I18nManager.forceRTL(true);

const { WiFiModule } = NativeModules;

const App = () => {
  const [ssid, setSsid] = useState('MyWiFiHotspot');
  const [password, setPassword] = useState('12345678');
  const [isActive, setIsActive] = useState(false);
  const [devices, setDevices] = useState([
    { id: '1', name: 'جهاز متصل (192.168.43.15)', ip: '192.168.43.15', mac: 'AA:BB:CC:DD:EE:FF' }
  ]);

  const toggleHotspot = () => {
    if (password.length < 8) {
      Alert.alert('خطأ', 'يجب أن تكون كلمة المرور 8 أحرف على الأقل');
      return;
    }

    // Call Native Bridge
    WiFiModule.setHotspotEnabled(!isActive, ssid, password)
      .then(result => {
        setIsActive(!isActive);
      })
      .catch(err => {
        Alert.alert('تنبيه', 'يرجى تفعيل البث يدوياً من الإعدادات بسبب قيود النظام');
        WiFiModule.openSettings();
      });
  };

  const renderDevice = ({ item }) => (
    <View style={styles.deviceCard}>
      <Text style={styles.deviceName}>{item.name}</Text>
      <Text style={styles.deviceDetails}>IP: {item.ip} | MAC: {item.mac}</Text>
      <View style={styles.actionButtons}>
        <TouchableOpacity style={styles.btnLimit}><Text style={styles.btnText}>تحديد</Text></TouchableOpacity>
        <TouchableOpacity style={styles.btnSpeed}><Text style={styles.btnText}>سرعة</Text></TouchableOpacity>
        <TouchableOpacity style={styles.btnBlock}><Text style={styles.btnText}>حظر</Text></TouchableOpacity>
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.statusLabel}>{isActive ? 'نشط' : 'غير نشط'}</Text>
      </View>

      <View style={styles.inputSection}>
        <Text style={styles.label}>اسم الشبكة:</Text>
        <TextInput
          style={styles.input}
          value={ssid}
          onChangeText={setSsid}
          placeholder="WiFi SSID"
        />

        <Text style={styles.label}>كلمة المرور:</Text>
        <TextInput
          style={styles.input}
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          placeholder="Password"
        />

        <TouchableOpacity
          style={[styles.mainButton, isActive && styles.btnActive]}
          onPress={toggleHotspot}
        >
          <Text style={styles.mainButtonText}>
            {isActive ? 'إيقاف نقطة الاتصال' : 'تفعيل نقطة الاتصال'}
          </Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.sectionTitle}>الأجهزة المتصلة:</Text>
      <FlatList
        data={devices}
        renderItem={renderDevice}
        keyExtractor={item => item.id}
        style={styles.list}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8f9fa', padding: 16 },
  header: { alignItems: 'center', marginVertical: 20 },
  statusLabel: { fontSize: 20, fontWeight: 'bold', color: '#007bff' },
  inputSection: { marginBottom: 24 },
  label: { fontSize: 14, marginBottom: 4, textAlign: 'right' },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    textAlign: 'right'
  },
  mainButton: {
    backgroundColor: '#007bff',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center'
  },
  btnActive: { backgroundColor: '#dc3545' },
  mainButtonText: { color: '#fff', fontWeight: 'bold', fontSize: 16 },
  sectionTitle: { fontSize: 18, fontWeight: 'bold', marginBottom: 12, textAlign: 'right' },
  deviceCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3
  },
  deviceName: { fontSize: 16, fontWeight: 'bold', textAlign: 'right' },
  deviceDetails: { fontSize: 12, color: '#666', textAlign: 'right', marginVertical: 4 },
  actionButtons: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 },
  btnLimit: { backgroundColor: '#6c757d', padding: 8, borderRadius: 6, flex: 1, marginHorizontal: 2 },
  btnSpeed: { backgroundColor: '#17a2b8', padding: 8, borderRadius: 6, flex: 1, marginHorizontal: 2 },
  btnBlock: { backgroundColor: '#dc3545', padding: 8, borderRadius: 6, flex: 1, marginHorizontal: 2 },
  btnText: { color: '#fff', fontSize: 12, textAlign: 'center' }
});

export default App;
