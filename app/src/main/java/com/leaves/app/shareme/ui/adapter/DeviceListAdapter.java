package com.leaves.app.shareme.ui.adapter;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import androidx.annotation.LayoutRes;

import android.view.View;
import android.widget.TextView;

import com.leaves.app.shareme.R;

import java.util.List;

/**
 * Created by Leaves on 2017/4/1.
 */

public class DeviceListAdapter extends BaseAdapter<WifiP2pDevice> {

    public DeviceListAdapter(List<WifiP2pDevice> data, @LayoutRes int itemLayoutRes) {
        super(data, itemLayoutRes);
    }

    @Override
    protected BaseViewHolder<WifiP2pDevice> creatingHolder(View view, Context context, int viewType) {
        return new BaseViewHolder<WifiP2pDevice>(view) {
            @Override
            public void setData(WifiP2pDevice wifiP2pDevice) {
                setTextView(R.id.tv_name, wifiP2pDevice.deviceName);
                TextView textView = (TextView) findView(R.id.tv_mac);
                textView.setText(String.format("(%1s)", wifiP2pDevice.deviceAddress));
            }
        };
    }
}

