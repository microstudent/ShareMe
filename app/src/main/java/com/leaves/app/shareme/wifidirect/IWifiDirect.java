package com.leaves.app.shareme.wifidirect;

import com.leaves.app.shareme.wifidirect.listener.OnConnectionChangeListener;

import java.util.Map;

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
    void setupSign(Map<String, String> params);

    void stopDiscover();

    /**
     * 设置Listener
     */
    void setOnConnectionChangeListener(OnConnectionChangeListener listener);
}
