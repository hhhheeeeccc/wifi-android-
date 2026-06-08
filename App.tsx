import React from 'react';
import {
  SafeAreaView,
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
} from 'react-native';
import { useMainViewModel } from './src/viewmodels/useMainViewModel';
import { DeviceCard } from './src/components/DeviceCard';

const App = () => {
  const {
    state,
    devices,
    setSsid,
    setPassword,
    toggleHotspot,
    toggleProxy,
    blockDevice
  } = useMainViewModel();

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.status}>حالة البث: {state.isActive ? 'نشط' : 'غير نشط'}</Text>
      </View>

      <View style={styles.form}>
        <Text style={styles.label}>اسم الشبكة:</Text>
        <TextInput
          style={styles.input}
          value={state.ssid}
          onChangeText={setSsid}
          placeholder="WiFi Name"
        />

        <Text style={styles.label}>كلمة المرور:</Text>
        <TextInput
          style={styles.input}
          value={state.password}
          onChangeText={setPassword}
          secureTextEntry
          placeholder="Password"
        />

        <TouchableOpacity
          style={[styles.mainBtn, state.isActive && styles.btnStop]}
          onPress={toggleHotspot}
        >
          <Text style={styles.btnText}>
            {state.isActive ? 'إيقاف البث' : 'تفعيل البث'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.proxyBtn} onPress={toggleProxy}>
          <Text style={styles.proxyBtnText}>
            {state.isProxyEnabled ? 'إخفاء معلومات البروكسي' : 'بث بدون روت (Proxy)'}
          </Text>
        </TouchableOpacity>
      </View>

      {state.isProxyEnabled && (
        <View style={styles.proxyInfo}>
          <Text style={styles.proxyTitle}>إعدادات الأجهزة الأخرى:</Text>
          <Text>المضيف: 192.168.43.1</Text>
          <Text>المنفذ: 8080</Text>
        </View>
      )}

      <Text style={styles.sectionTitle}>الأجهزة المتصلة:</Text>
      <FlatList
        data={devices}
        renderItem={({ item }) => <DeviceCard device={item} onBlock={blockDevice} />}
        keyExtractor={item => item.id}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#f5f5f5' },
  header: { alignItems: 'center', marginBottom: 20 },
  status: { fontSize: 18, fontWeight: 'bold', color: '#333' },
  form: { marginBottom: 20 },
  label: { textAlign: 'right', marginBottom: 5, fontSize: 14 },
  input: { backgroundColor: '#fff', padding: 12, borderRadius: 8, marginBottom: 12, textAlign: 'right', borderWidth: 1, borderColor: '#ddd' },
  mainBtn: { backgroundColor: '#28a745', padding: 15, borderRadius: 8, alignItems: 'center' },
  btnStop: { backgroundColor: '#dc3545' },
  btnText: { color: '#fff', fontWeight: 'bold' },
  proxyBtn: { marginTop: 10, alignItems: 'center' },
  proxyBtnText: { color: '#007bff', textDecorationLine: 'underline' },
  proxyInfo: { backgroundColor: '#e9ecef', padding: 12, borderRadius: 8, marginBottom: 20 },
  proxyTitle: { fontWeight: 'bold', marginBottom: 5 },
  sectionTitle: { fontSize: 18, fontWeight: 'bold', marginBottom: 10, textAlign: 'right' }
});

export default App;
