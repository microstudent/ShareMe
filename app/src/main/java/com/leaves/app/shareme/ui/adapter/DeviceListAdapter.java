package com.leaves.app.shareme.ui.adapter;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leaves.app.shareme.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Leaves on 2017/4/1.
 */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<WifiP2pDevice> mDevices;

    private Context mContext;

    private LayoutInflater mInflater;

    public DeviceListAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mDevices = new ArrayList<>();
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.layout_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.mNameView.setText(mDevices.get(position).deviceName);
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public void addNewDevice(WifiP2pDevice wifiP2pDevice) {
        if (!mDevices.contains(wifiP2pDevice)) {
            mDevices.add(wifiP2pDevice);
            notifyItemInserted(mDevices.size() - 1);
        }
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_name)
        TextView mNameView;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

