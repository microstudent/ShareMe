package com.leaves.app.shareme.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;


/**
 * broadcast接收关于WifiDirect的变化
 * Created by leaves on 17-1-23.
 */
public class WifiDirectReceiver extends BroadcastReceiver {
    private OnWifiDirectStateChangeListener mOnWifiDirectStateChangeListener;


    public void setOnWifiDirectStateChangeListener(OnWifiDirectStateChangeListener onWifiDirectStateChangeListener) {
        mOnWifiDirectStateChangeListener = onWifiDirectStateChangeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mOnWifiDirectStateChangeListener == null) {
            return;
        }
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            //wifi direct 变化
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                mOnWifiDirectStateChangeListener.onWifiDirectStateChange(true);
            } else {
                // Wi-Fi P2P is not enabled
                mOnWifiDirectStateChangeListener.onWifiDirectStateChange(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            //peers 列表变化，自行通过requestPeers获取新列表
            mOnWifiDirectStateChangeListener.onPeersChange();
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //有人断线or连接
            mOnWifiDirectStateChangeListener.onConnectionChange((WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO),
                    (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO));
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            //设备发生变化，例如设备名发生变化
            mOnWifiDirectStateChangeListener.onDeviceDetailChange();
        }
    }


    protected interface OnWifiDirectStateChangeListener {
        /**
         * WifiDirect功能开关的listener
         */
        void onWifiDirectStateChange(boolean enable);

        /**
         * 发现周边的新设备等
         */
        void onPeersChange();

        /**
         * 设备连接或断开
         */
        void onConnectionChange(WifiP2pInfo wifiP2pInfo, NetworkInfo networkInfo);

        void onDeviceDetailChange();
    }
}
