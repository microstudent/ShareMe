package com.leaves.app.shareme.presenter;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.support.v7.app.AppCompatActivity;

import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.contract.WifiDirectionContract;
import com.leaves.sdk.wifidirect.WifiDirect;
import com.leaves.sdk.wifidirect.listener.OnConnectionChangeListener;
import com.leaves.sdk.wifidirect.listener.OnDeviceDetailChangeListener;
import com.leaves.sdk.wifidirect.listener.OnServiceFoundListener;

import java.util.HashMap;
import java.util.Map;

import static com.leaves.app.shareme.Constant.WifiDirect.INSTANCE_NAME;
import static com.leaves.app.shareme.Constant.WifiDirect.SERVICE_NAME;

/**
 * Created by Leaves on 2017/3/29.
 */

public class WifiDirectionPresenter implements WifiDirectionContract.Presenter, OnConnectionChangeListener, OnDeviceDetailChangeListener, OnServiceFoundListener {

    private final WifiDirectionContract.View mView;
    private WifiDirect mWifiDirect;

    private StringBuilder mPassword;

    public WifiDirectionPresenter(WifiDirectionContract.View view, AppCompatActivity rootActivity) {
        mView = view;
        mWifiDirect = new WifiDirect(rootActivity, INSTANCE_NAME, SERVICE_NAME);
        mPassword = new StringBuilder();
        mWifiDirect.setOnConnectionChangeListener(this);
        mWifiDirect.setOnDeviceDetailChangeListener(this);
        mWifiDirect.setOnServiceFoundListener(this);
    }


    @Override
    public void start() {
    }


    @Override
    public void appendPassword(String number) {
        if (mPassword.length() >= 4) {
            return;
        }
        mPassword.append(number);
        if (mPassword.length() >= 4) {
            startDiscovery();
        }
    }

    private void startDiscovery() {
        Map<String, String> params = new HashMap<>();
        params.put(Constant.WifiDirect.KEY_PASSWORD, mPassword.toString());
        params.put(Constant.WifiDirect.KEY_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        mWifiDirect.setupSignAndScan(params);
    }

    @Override
    public void onConnectionChange(WifiP2pDeviceList deviceList) {

    }

    @Override
    public void onDeviceDetailChanged(WifiP2pDevice device) {

    }

    @Override
    public void onServiceFound(WifiP2pDevice device, WifiDirect.ServiceResponse response) {
        if (isPasswordCorrect(response)) {
            mView.showToast(response.wifiP2pDevice.deviceName);
        }
    }

    private boolean isPasswordCorrect(WifiDirect.ServiceResponse response) {
        Map<String, String> params = response.txtRecordMap;
        if (params != null) {
            if (params.containsKey(Constant.WifiDirect.KEY_PASSWORD)) {
                if (params.get(Constant.WifiDirect.KEY_PASSWORD).equals(mPassword.toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}
