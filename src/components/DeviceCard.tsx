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
      <View style={styles.header}>
        <View style={styles.iconContainer}>
          <Text style={styles.iconText}>{device.name.charAt(0).toUpperCase()}</Text>
        </View>
        <View style={styles.info}>
          <Text style={styles.name}>{device.name}</Text>
          <Text style={styles.details}>IP: {device.ip}</Text>
          <Text style={styles.details}>MAC: {device.mac}</Text>
        </View>
      </View>

      <View style={styles.divider} />

      <View style={styles.actions}>
        <TouchableOpacity style={styles.actionBtn}>
          <Text style={styles.actionBtnText}>التفاصيل</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn}>
          <Text style={styles.actionBtnText}>السرعة</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.actionBtn, device.isBlocked ? styles.btnUnblock : styles.btnBlock]}
          onPress={() => onBlock(device.id)}
        >
          <Text style={styles.actionBtnText}>{device.isBlocked ? 'فك الحظر' : 'حظر'}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 15,
    padding: 15,
    marginBottom: 15,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  header: { flexDirection: 'row-reverse', alignItems: 'center' },
  iconContainer: {
    width: 45,
    height: 45,
    borderRadius: 22.5,
    backgroundColor: '#e8eaf6',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 15
  },
  iconText: { fontSize: 20, fontWeight: 'bold', color: '#1a237e' },
  info: { flex: 1 },
  name: { fontSize: 16, fontWeight: 'bold', color: '#333', textAlign: 'right', marginBottom: 2 },
  details: { fontSize: 12, color: '#666', textAlign: 'right' },
  divider: { height: 1, backgroundColor: '#f0f0f0', marginVertical: 12 },
  actions: { flexDirection: 'row-reverse', justifyContent: 'space-between' },
  actionBtn: {
    flex: 1,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#f8f9fa',
    marginHorizontal: 5,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#eee'
  },
  actionBtnText: { fontSize: 12, fontWeight: '600', color: '#555' },
  btnBlock: { backgroundColor: '#fff0f0', borderColor: '#ffcdd2' },
  btnUnblock: { backgroundColor: '#e8f5e9', borderColor: '#c8e6c9' },
});
