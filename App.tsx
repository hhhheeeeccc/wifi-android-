import React from 'react';
import {
  SafeAreaView,
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  StatusBar,
  ScrollView,
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
      <StatusBar barStyle="light-content" backgroundColor="#1a237e" />

      <View style={styles.header}>
        <Text style={styles.headerTitle}>بث الشبكة (Direct Edition)</Text>
        <View style={styles.statusBadge}>
          <View style={[styles.statusDot, state.isActive ? styles.dotActive : styles.dotInactive]} />
          <Text style={styles.statusText}>{state.isActive ? 'نشط الآن' : 'غير نشط'}</Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.card}>
          <Text style={styles.cardTitle}>إعدادات البث</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>اسم الشبكة (SSID)</Text>
            <TextInput
              style={styles.input}
              value={state.ssid}
              onChangeText={setSsid}
              placeholder="SSID"
              placeholderTextColor="#999"
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>كلمة المرور</Text>
            <TextInput
              style={styles.input}
              value={state.password}
              onChangeText={setPassword}
              secureTextEntry
              placeholder="Password"
              placeholderTextColor="#999"
            />
          </View>

          <TouchableOpacity
            style={[styles.mainBtn, state.isActive && styles.btnStop]}
            onPress={toggleHotspot}
            activeOpacity={0.8}
          >
            <Text style={styles.btnText}>
              {state.isActive ? 'إيقاف البث المباشر' : 'تفعيل البث المباشر'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.proxyBtn} onPress={toggleProxy}>
            <Text style={styles.proxyBtnText}>
              {state.isProxyEnabled ? 'إخفاء إعدادات البروكسي' : 'بث بدون روت (Proxy Mode)'}
            </Text>
          </TouchableOpacity>
        </View>

        {state.isProxyEnabled && (
          <View style={[styles.card, styles.proxyCard]}>
            <Text style={styles.cardTitle}>إعدادات الأجهزة الأخرى</Text>
            <View style={styles.proxyRow}>
              <Text style={styles.proxyLabel}>عنوان المضيف:</Text>
              <Text style={styles.proxyValue}>192.168.43.1</Text>
            </View>
            <View style={styles.proxyRow}>
              <Text style={styles.proxyLabel}>المنفذ:</Text>
              <Text style={styles.proxyValue}>8080</Text>
            </View>
            <Text style={styles.proxyTip}>يجب ضبط البروكسي يدوياً في إعدادات الواي فاي للجهاز المتصل.</Text>
          </View>
        )}

        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>الأجهزة المتصلة ({devices.length})</Text>
        </View>

        {devices.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyText}>لا يوجد أجهزة متصلة حالياً</Text>
          </View>
        ) : (
          devices.map((item) => (
            <DeviceCard key={item.id} device={item} onBlock={blockDevice} />
          ))
        )}
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f0f2f5' },
  header: {
    backgroundColor: '#1a237e',
    paddingTop: 20,
    paddingBottom: 30,
    paddingHorizontal: 20,
    borderBottomLeftRadius: 30,
    borderBottomRightRadius: 30,
    alignItems: 'center'
  },
  headerTitle: { fontSize: 22, fontWeight: 'bold', color: '#fff', marginBottom: 10 },
  statusBadge: {
    flexDirection: 'row',
    backgroundColor: 'rgba(255,255,255,0.2)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    alignItems: 'center'
  },
  statusDot: { width: 10, height: 10, borderRadius: 5, marginRight: 8 },
  dotActive: { backgroundColor: '#4caf50' },
  dotInactive: { backgroundColor: '#f44336' },
  statusText: { color: '#fff', fontSize: 14, fontWeight: '500' },
  scrollContent: { padding: 20 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 20,
    marginBottom: 20,
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4
  },
  cardTitle: { fontSize: 18, fontWeight: 'bold', color: '#333', marginBottom: 15, textAlign: 'right' },
  inputGroup: { marginBottom: 15 },
  label: { fontSize: 14, color: '#666', marginBottom: 5, textAlign: 'right' },
  input: {
    backgroundColor: '#f8f9fa',
    padding: 12,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#e0e0e0',
    textAlign: 'right',
    fontSize: 16,
    color: '#333'
  },
  mainBtn: {
    backgroundColor: '#28a745',
    paddingVertical: 15,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 10
  },
  btnStop: { backgroundColor: '#d32f2f' },
  btnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  proxyBtn: { marginTop: 15, alignItems: 'center' },
  proxyBtnText: { color: '#1a237e', fontSize: 14, fontWeight: '600', textDecorationLine: 'underline' },
  proxyCard: { backgroundColor: '#e8eaf6' },
  proxyRow: { flexDirection: 'row-reverse', justifyContent: 'space-between', marginBottom: 8 },
  proxyLabel: { color: '#555', fontSize: 14 },
  proxyValue: { fontWeight: 'bold', color: '#1a237e', fontSize: 14 },
  proxyTip: { fontSize: 12, color: '#777', textAlign: 'right', marginTop: 10, fontStyle: 'italic' },
  sectionHeader: { marginBottom: 15 },
  sectionTitle: { fontSize: 18, fontWeight: 'bold', color: '#333', textAlign: 'right' },
  emptyState: { alignItems: 'center', paddingVertical: 40 },
  emptyText: { color: '#999', fontSize: 16 }
});

export default App;
