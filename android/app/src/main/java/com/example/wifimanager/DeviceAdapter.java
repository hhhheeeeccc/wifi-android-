package com.example.wifimanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wifimanager.databinding.DeviceItemBinding;
import com.example.wifimanager.databinding.DialogLimitBinding;
import com.example.wifimanager.databinding.DialogSpeedBinding;
import com.example.wifimanager.model.Device;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<Device> devices;
    private final Context context;

    public DeviceAdapter(List<Device> devices, Context context) {
        this.devices = devices;
        this.context = context;
    }

    public void updateDevices(List<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DeviceItemBinding binding = DeviceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DeviceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final DeviceItemBinding binding;

        public DeviceViewHolder(DeviceItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Device device) {
            binding.deviceName.setText(device.getDeviceName());
            binding.deviceDetails.setText(String.format("IP: %s | MAC: %s", device.getIpAddress(), device.getMacAddress()));

            binding.btnLimit.setOnClickListener(v -> showLimitDialog(device));
            binding.btnSpeed.setOnClickListener(v -> showSpeedDialog(device));
            binding.btnBlock.setText(context.getString(device.isBlocked() ? R.string.unblock_user : R.string.block_user));
            binding.btnBlock.setOnClickListener(v -> {
                device.setBlocked(!device.isBlocked());
                binding.btnBlock.setText(context.getString(device.isBlocked() ? R.string.unblock_user : R.string.block_user));
            });

            if (device.getDataLimit() > 0) {
                binding.dataProgress.setVisibility(View.VISIBLE);
                int progress = (int) ((device.getUsedData() * 100) / device.getDataLimit());
                binding.dataProgress.setProgress(progress);
            } else {
                binding.dataProgress.setVisibility(View.GONE);
            }
        }

        private void showLimitDialog(Device device) {
            DialogLimitBinding limitBinding = DialogLimitBinding.inflate(LayoutInflater.from(context));
            new AlertDialog.Builder(context)
                .setTitle(R.string.limit_data)
                .setView(limitBinding.getRoot())
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String input = limitBinding.limitInput.getText().toString();
                    if (!input.isEmpty()) {
                        device.setDataLimit(Long.parseLong(input));
                        notifyItemChanged(getAdapterPosition());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }

        private void showSpeedDialog(Device device) {
            DialogSpeedBinding speedBinding = DialogSpeedBinding.inflate(LayoutInflater.from(context));
            new AlertDialog.Builder(context)
                .setTitle(R.string.speed_limit)
                .setView(speedBinding.getRoot())
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String input = speedBinding.speedInput.getText().toString();
                    if (!input.isEmpty()) {
                        device.setSpeedLimit(Integer.parseInt(input));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }
    }
}
