package com.leaves.app.shareme.contract;

import android.net.wifi.p2p.WifiP2pDevice;

import com.leaves.app.shareme.BasePresenter;
import com.leaves.app.shareme.BaseView;

/**
 * Created by Leaves on 2017/3/29.
 */

public interface WifiDirectionContract {
    interface View extends BaseView {
        void onStartDiscover();

        void onDeviceFound(WifiP2pDevice wifiP2pDevice);

        void startServer();

        void setServerIp();
    }

    interface Presenter extends BasePresenter {
        void appendPassword(String number);

        void cancelSearch();
    }
}
