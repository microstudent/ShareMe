package com.leaves.app.shareme.wifidirect.listener;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.List;

/**
 * 设备连接发生变化
 * Created by leaves on 17-1-24.
 */
public interface OnConnectionChangeListener {
    void onConnectionChange(List<WifiP2pDevice> deviceList);
}
