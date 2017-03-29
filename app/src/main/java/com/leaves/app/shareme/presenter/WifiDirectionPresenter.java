package com.leaves.app.shareme.presenter;

import android.support.v7.app.AppCompatActivity;

import com.leaves.app.shareme.contract.WifiDirectionContract;
import com.leaves.sdk.wifidirect.WifiDirect;

/**
 * Created by Leaves on 2017/3/29.
 */

public class WifiDirectionPresenter implements WifiDirectionContract.Presenter {

    private static final String INSTANCE_NAME = "ShareMe";
    private static final String SERVICE_NAME = "sync music player";

    private WifiDirect mWifiDirect;

    public WifiDirectionPresenter(AppCompatActivity rootActivity) {
        mWifiDirect = new WifiDirect(rootActivity, INSTANCE_NAME, SERVICE_NAME);
    }


    @Override
    public void start() {

    }

    @Override
    public void setPassword(String password) {

    }
}
