package com.leaves.app.shareme.contract;

import android.net.wifi.p2p.WifiP2pDevice;

import com.leaves.app.shareme.BasePresenter;
import com.leaves.app.shareme.BaseView;

import java.util.ArrayList;

/**
 * Created by Leaves on 2017/4/19.
 */

public interface MainActivityContract {
    interface View extends BaseView{

        void onSearching();

        void setupFragment(int mode);
    }

    interface Presenter extends BasePresenter {
        void onDialpadClick(int position, String number);

        boolean isServer();

        void cancelSearch();

        ArrayList<WifiP2pDevice> getDeviceList();
    }
}
