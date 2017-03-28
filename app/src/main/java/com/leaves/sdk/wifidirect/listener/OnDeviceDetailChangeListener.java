package com.leaves.sdk.wifidirect.listener;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by zhangshuyue on 17-3-1.
 */
public interface OnDeviceDetailChangeListener {
    void onDeviceDetailChanged(WifiP2pDevice device);
}
