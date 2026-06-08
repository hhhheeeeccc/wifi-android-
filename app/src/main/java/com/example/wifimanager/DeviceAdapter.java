package com.example.wifimanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private List<Device> devices;
    private OnDeviceActionListener listener;

    public interface OnDeviceActionListener {
        void onBlock(Device device);
        void onLimit(Device device);
        void onSpeedLimit(Device device);
    }

    public DeviceAdapter(List<Device> devices, OnDeviceActionListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.deviceName.setText(device.getDeviceName());
        holder.deviceDetails.setText("IP: " + device.getIpAddress() + " | MAC: " + device.getMacAddress());

        holder.btnBlock.setText(device.isBlocked() ? "إلغاء الحظر" : "حظر");

        holder.btnBlock.setOnClickListener(v -> listener.onBlock(device));
        holder.btnLimit.setOnClickListener(v -> listener.onLimit(device));
        holder.btnSpeed.setOnClickListener(v -> listener.onSpeedLimit(device));

        if (device.getDataLimit() > 0) {
            holder.dataProgress.setVisibility(View.VISIBLE);
            holder.dataProgress.setMax((int) device.getDataLimit());
            holder.dataProgress.setProgress((int) device.getUsedData());
        } else {
            holder.dataProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, deviceDetails;
        Button btnLimit, btnBlock, btnSpeed;
        ProgressBar dataProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceDetails = itemView.findViewById(R.id.deviceDetails);
            btnLimit = itemView.findViewById(R.id.btnLimit);
            btnBlock = itemView.findViewById(R.id.btnBlock);
            btnSpeed = itemView.findViewById(R.id.btnSpeed);
            dataProgress = itemView.findViewById(R.id.dataProgress);
        }
    }
}
