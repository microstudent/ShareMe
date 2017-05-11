package com.leaves.app.shareme.presenter;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.support.v4.app.FragmentManager;
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
    private long mLocalTimeStamp;
    private WifiP2pDeviceList mDeviceList;

    public WifiDirectionPresenter(WifiDirectionContract.View view, AppCompatActivity rootActivity) {
        this(view, rootActivity.getSupportFragmentManager(), rootActivity.getApplicationContext());
    }

    public WifiDirectionPresenter(WifiDirectionContract.View view, FragmentManager manager, Context context) {
        mView = view;
        mWifiDirect = WifiDirect.getInstance(manager, context, INSTANCE_NAME, SERVICE_NAME);
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

    @Override
    public void clearPassword() {
        if (mPassword.length() >= 1) {
            mPassword.delete(0, mPassword.length());
        }
    }

    @Override
    public void backspacePassword() {
        if (mPassword.length() >= 1) {
            mPassword.deleteCharAt(mPassword.length() - 1);
        }
    }

    @Override
    public void cancelSearch() {
        if (mWifiDirect != null) {
            mWifiDirect.stopDiscover();
        }
    }

    private void startDiscovery() {
        Map<String, String> params = new HashMap<>();
        params.put(Constant.WifiDirect.KEY_PASSWORD, mPassword.toString());
        mLocalTimeStamp = System.currentTimeMillis();
        params.put(Constant.WifiDirect.KEY_TIMESTAMP, String.valueOf(mLocalTimeStamp));
        mWifiDirect.setupSignAndScan(params);
        mView.onStartDiscover();
    }

    @Override
    public void onConnectionChange(WifiP2pDeviceList deviceList) {
        if (mWifiDirect.isGroupOwner()) {
            mView.showToast("I am group owner");
            mView.startAsServer();
        } else {
            if (mWifiDirect.getGroupOwnerIp() != null) {
                mView.startAsClient(mWifiDirect.getGroupOwnerIp());
            } else {
                mView.startAsUndefined();
            }
        }
        mDeviceList = deviceList;
    }

    public WifiP2pDeviceList getDeviceList() {
        return mDeviceList;
    }

    @Override
    public void onDeviceDetailChanged(WifiP2pDevice device) {

    }

    @Override
    public void onServiceFound(WifiP2pDevice device, WifiDirect.ServiceResponse response) {
        if (isPasswordCorrect(response)) {
            mView.onDeviceFound(response.wifiP2pDevice);
            //时间晚的去连接早的
            if (mLocalTimeStamp > Long.parseLong(response.txtRecordMap.get(Constant.WifiDirect.KEY_TIMESTAMP))) {
                mWifiDirect.connectTo(device, getGroupIntent(response.txtRecordMap.get(Constant.WifiDirect.KEY_TIMESTAMP)));
            }
        }
    }

    private int getGroupIntent(String timeStamp) {
        long deviceTimeStamp = Long.parseLong(timeStamp);
        if (mLocalTimeStamp > deviceTimeStamp) {
            //本地的比找到的这台手机慢
            return 6;
        } else {
            return 8;
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
