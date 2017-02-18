package com.leaves.app.shareme.wifidirect;

import android.net.wifi.p2p.WifiP2pDevice;
import com.leaves.app.shareme.wifidirect.listener.OnConnectionChangeListener;

import java.util.Map;

/**
 * Created by leaves on 17-1-23.
 */
public interface IWifiDirect {

    /**
     * 设置标识
     */
    void setupSignAndScan(Map<String, String> params);

    void stopDiscover();


    void connectTo(WifiP2pDevice device, int groupOwnerIntent);

    /**
     * 设置Listener
     */
    void setOnConnectionChangeListener(OnConnectionChangeListener listener);
}
