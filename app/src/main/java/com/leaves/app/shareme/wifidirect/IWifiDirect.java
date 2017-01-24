package com.leaves.app.shareme.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.List;

/**
 * Created by leaves on 17-1-23.
 */
public interface IWifiDirect {
    /**
     * 根据setupSign设置的标识，扫描并自动连接合适的设备
     */
    void scanAndConnect();

    /**
     * 设置标识
     */
    void setupSign(String key);

    /**
     * 设置Listener
     */
    void setOnConnectionChangeListener(OnConnectionChangeListener listener);
}
