package com.leaves.app.shareme.presenter;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.v4.app.FragmentManager;

import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.contract.MainActivityContract;
import com.leaves.app.shareme.contract.WifiDirectionContract;
import com.leaves.app.shareme.service.MusicClientService;
import com.leaves.app.shareme.service.MusicServerService;

/**
 * Created by Leaves on 2017/4/19.
 */

public class MainPresenter implements MainActivityContract.Presenter, WifiDirectionContract.View {
    public static final int MODE_STARTUP = 0;
    public static final int MODE_CONNECTED = 1;

    private final Context mContext;
    public WifiDirectionPresenter mWifiDirectionPresenter;
    private MainActivityContract.View mView;
    private int mMode = MODE_STARTUP;
    private boolean isServiceStarted;
    private boolean isServer;

    public MainPresenter(MainActivityContract.View view, FragmentManager manager, Context context) {
        mWifiDirectionPresenter = new WifiDirectionPresenter(this, manager, context);
        mView = view;
        mContext = context;
    }

    @Override
    public void start() {
        mWifiDirectionPresenter.start();
    }

    @Override
    public void showToast(String msg) {
        mView.showToast(msg);
    }

    @Override
    public void onStartDiscover() {
        mView.onSearching();
    }

    @Override
    public void onDeviceFound(WifiP2pDevice wifiP2pDevice) {
        mMode = MODE_CONNECTED;
    }

    @Override
    public void startAsServer() {
        mMode = MODE_CONNECTED;
        isServer = true;
        mView.setupFragment(mMode);
        if (!isServiceStarted) {
            Intent intent = new Intent(mContext, MusicServerService.class);
            mContext.startService(intent);
            isServiceStarted = true;
        }
    }

    @Override
    public void startAsClient(String serverIp) {
        mMode = MODE_CONNECTED;
        isServer = false;
        mView.setupFragment(mMode);
        if (!isServiceStarted) {
            Intent intent = new Intent(mContext, MusicClientService.class);
            intent.putExtra(Constant.Intent.SERVER_IP, serverIp);
            mContext.startService(intent);
            isServiceStarted = true;
        }
    }

    @Override
    public void startAsUndefined() {
        mMode = MODE_STARTUP;
        isServer = false;
        mView.setupFragment(mMode);
    }

    @Override
    public void onDialpadClick(int position, String number) {
        if (position == 9) {
            //clearPassword all
            mWifiDirectionPresenter.clearPassword();
        } else if (position == 11) {
            //backspace
            mWifiDirectionPresenter.backspacePassword();
        } else {
            mWifiDirectionPresenter.appendPassword(number);
        }
    }

    @Override
    public boolean isServer() {
        return isServer;
    }

}
