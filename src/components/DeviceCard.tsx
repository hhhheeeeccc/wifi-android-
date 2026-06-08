import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Device } from '../models/types';

interface Props {
  device: Device;
  onBlock: (id: string) => void;
}

export const DeviceCard: React.FC<Props> = ({ device, onBlock }) => {
  return (
    <View style={styles.card}>
      <Text style={styles.name}>{device.name}</Text>
      <Text style={styles.details}>IP: {device.ip} | MAC: {device.mac}</Text>
      <View style={styles.actions}>
        <TouchableOpacity style={styles.btn}><Text style={styles.btnText}>تحديد</Text></TouchableOpacity>
        <TouchableOpacity style={styles.btn}><Text style={styles.btnText}>سرعة</Text></TouchableOpacity>
        <TouchableOpacity
          style={[styles.btn, device.isBlocked && styles.btnActive]}
          onPress={() => onBlock(device.id)}
        >
          <Text style={styles.btnText}>{device.isBlocked ? 'فك الحظر' : 'حظر'}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: { backgroundColor: '#fff', padding: 16, borderRadius: 12, marginBottom: 12, elevation: 2 },
  name: { fontSize: 16, fontWeight: 'bold', textAlign: 'right' },
  details: { fontSize: 12, color: '#666', textAlign: 'right', marginVertical: 4 },
  actions: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 },
  btn: { backgroundColor: '#007bff', padding: 8, borderRadius: 6, flex: 1, marginHorizontal: 2 },
  btnActive: { backgroundColor: '#dc3545' },
  btnText: { color: '#fff', fontSize: 12, textAlign: 'center' }
});
