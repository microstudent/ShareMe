package com.leaves.app.shareme.wifidirect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.*;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Wifi Direct控制类,生命周期和rootActivity绑定，因为其只需要一个实例，并不需要多个实例对象，所以或许可以考虑使用root？
 * Created by leaves on 17-1-23.
 */
public class WifiDirect implements WifiDirectReceiver.OnWifiDirectStateChangeListener, LifecycleListener {
    private static final String FRAGMENT_TAG = "com.leaves.app.shareme.wifidirect";

    private final IntentFilter mIntentFilter;
    private Context mContext;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectReceiver mWifiDirectReceiver;

    public WifiDirect(AppCompatActivity rootActivity) {
        mContext = rootActivity.getApplicationContext();

        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);
        mWifiDirectReceiver = new WifiDirectReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mWifiDirectReceiver.setOnWifiDirectStateChangeListener(this);
        setupShadowFragment(rootActivity);
    }


    public void scan() {
        Map<String, String> info = new HashMap<>();
        info.put("a", "1");
        WifiP2pDnsSdServiceInfo wifiP2pDnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("_test", "_ShareMe", info);
        mWifiP2pManager.addLocalService(mChannel, wifiP2pDnsSdServiceInfo, null);


        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                log("onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                log("onFailure:" + reason);
            }
        });
    }


    private void setupShadowFragment(AppCompatActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        ShadowFragment current = getShadowFragment(manager);
        current.getLifecycle().addListener(this);
    }

    static ShadowFragment getShadowFragment(final FragmentManager fm) {
        ShadowFragment current = (ShadowFragment) fm.findFragmentByTag(
                FRAGMENT_TAG);
        if (current == null) {
            current = new ShadowFragment();
            fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        }
        return current;
    }


    @Override
    public void onWifiDirectStateChange(boolean enable) {
        log("onWifiDirectStateChange");
    }

    @Override
    public void onPeersChange() {
        mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                for (WifiP2pDevice device : peers.getDeviceList()) {
                    log(device.primaryDeviceType);
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;
                    mWifiP2pManager.connect(mChannel, config, null);
                }
            }
        });
    }

    @Override
    public void onConnectionChange(WifiP2pInfo wifiP2pInfo, NetworkInfo networkInfo) {
        if (wifiP2pInfo.groupOwnerAddress != null) {
            log("onConnect" + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        }
    }

    @Override
    public void onDeviceDetailChange() {
        log("onDeviceDetailChange");
    }

    @Override
    public void onResume() {
        mContext.registerReceiver(mWifiDirectReceiver, mIntentFilter);
        log("onResume");
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mWifiDirectReceiver);
        log("onPause");
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        mContext = null;
    }


    private void log(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    public static class ShadowFragment extends Fragment {
        private ShadowFragment rootRequestManagerFragment;

        ActivityFragmentLifecycle lifecycle;
        public ShadowFragment() {
            this(new ActivityFragmentLifecycle());
        }
        @SuppressLint("ValidFragment")
        public ShadowFragment(ActivityFragmentLifecycle lifecycle) {
            this.lifecycle = lifecycle;
        }
        ActivityFragmentLifecycle getLifecycle() {
            return lifecycle;
        }


        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            rootRequestManagerFragment = getShadowFragment(getActivity().getSupportFragmentManager());
        }


        @Override
        public void onDetach() {
            super.onDetach();
            if (rootRequestManagerFragment != null) {
                rootRequestManagerFragment = null;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            lifecycle.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            lifecycle.onPause();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            lifecycle.onDestroy();
        }
    }
}

