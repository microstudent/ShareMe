package com.leaves.app.shareme.wifidirect.listener;

import android.net.wifi.p2p.WifiP2pDevice;
import com.leaves.app.shareme.wifidirect.WifiDirect;

/**
 * Created by zhangshuyue on 17-2-18.
 */
public interface OnServiceFoundListener {
    void onServiceFound(WifiP2pDevice device, WifiDirect.ServiceResponse response);
}
