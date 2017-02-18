package com.leaves.app.shareme.wifidirect;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Wifi Direct控制类,生命周期和rootActivity绑定，因为其只需要一个实例，并不需要多个实例对象，所以或许可以考虑使用root？
 * Created by leaves on 17-1-23.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class WifiDirect implements IWifiDirect, WifiDirectReceiver.OnWifiDirectStateChangeListener, LifecycleListener {
    private static final String FRAGMENT_TAG = "com.leaves.app.shareme.wifidirect.shadowfragment";
    public static final String INSTANCE_NAME = "shareMe";
    public static final String SERVICE_TYPE = "shareMe";

    private static final long SERVICE_BROADCASTING_INTERVAL = 10;//sec
    private static final long SERVICE_DISCOVERING_INTERVAL = 10;//sec

    private IntentFilter mIntentFilter;
    private Context mContext;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectReceiver mWifiDirectReceiver;
    private CompositeDisposable mDisposables;

    private WifiP2pDnsSdServiceRequest mWifiP2pServiceRequest;
    private PublishSubject<Integer> mSetSignSubject;
    private PublishSubject<Integer> mDiscoverSubject;

    public WifiDirect(AppCompatActivity rootActivity) {
        mContext = rootActivity.getApplicationContext();
        initWifiP2p();
        initReceiver();
        setupShadowFragment(rootActivity);//绑定生命周期
        mDisposables = new CompositeDisposable();
    }

    @Override
    public void scanAndConnect() {
        prepareServiceDiscovery();
        mDiscoverSubject = PublishSubject.create();
        mDiscoverSubject.subscribe(new Consumer<Integer>() {
            static final int TYPE_DISCOVER_SERVICES = 3;
            static final int TYPE_ADD_SERVICE_REQUEST = 2;
            static final int TYPE_REMOVE_SERVICE_REQUEST = 1;
            static final int TYPE_DEFAULT = 0;
            @Override
            public void accept(Integer integer) throws Exception {
                switch (integer) {
                    case TYPE_DEFAULT:
                        if (mWifiP2pServiceRequest != null) {
                            mWifiP2pManager.removeServiceRequest(mChannel, mWifiP2pServiceRequest, new RxActionListener(mDiscoverSubject, TYPE_REMOVE_SERVICE_REQUEST));
                        }
                        break;
                    case TYPE_REMOVE_SERVICE_REQUEST:
                        mWifiP2pManager.addServiceRequest(mChannel, mWifiP2pServiceRequest, new RxActionListener(mDiscoverSubject, TYPE_ADD_SERVICE_REQUEST));
                        break;
                    case TYPE_ADD_SERVICE_REQUEST:
                        showToast("discovering services");
                        mWifiP2pManager.discoverServices(mChannel, new RxActionListener(mDiscoverSubject, TYPE_DISCOVER_SERVICES));
                        mDisposables.add(mDiscoverSubject.delay(SERVICE_DISCOVERING_INTERVAL, TimeUnit.SECONDS).repeat().subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                mWifiP2pManager.discoverServices(mChannel, new RxActionListener(mDiscoverSubject, TYPE_DISCOVER_SERVICES));
                            }
                        }));
                        break;
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                showToast(throwable.toString());
            }
        });
        mDiscoverSubject.onNext(0);
    }

    private void prepareServiceDiscovery() {
        mWifiP2pManager.setDnsSdResponseListeners(mChannel,
                new WifiP2pManager.DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {
                        // do all the things you need to do with detected service
                        showToast(instanceName);
                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        // do all the things you need to do with detailed information about detected service
                        showToast(record.toString());
                    }
                });
        mWifiP2pServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
    }

    @Override
    public void setupSign(final String key) {
        mSetSignSubject = PublishSubject.create();
        mSetSignSubject.subscribe(new Consumer<Integer>() {
            static final int TYPE_CLEAR_LOCAL_SERVICES = 1;
            static final int TYPE_ADD_LOCAL_SERVICE = 2;

            @Override
            public void accept(Integer integer) throws Exception {
                switch (integer) {
                    case TYPE_CLEAR_LOCAL_SERVICES:
                        Map<String, String> txt = new HashMap<>();
                        txt.put("key", key);
                        WifiP2pDnsSdServiceInfo info = WifiP2pDnsSdServiceInfo.newInstance(INSTANCE_NAME, SERVICE_TYPE, txt);
                        mWifiP2pManager.addLocalService(mChannel, info, new RxActionListener(mSetSignSubject, TYPE_ADD_LOCAL_SERVICE));
                        break;
                    case TYPE_ADD_LOCAL_SERVICE:
                        //持久扫描
                        startServiceBroadcasting();
                        break;
                    default:
                        mWifiP2pManager.clearLocalServices(mChannel, new RxActionListener(mSetSignSubject, TYPE_CLEAR_LOCAL_SERVICES));
                        break;
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                showToast(throwable.toString());
            }
        });
        mSetSignSubject.onNext(0);
    }

    /**
     * 启动SERVICE广播
     */
    private void startServiceBroadcasting() {
        scanAndConnect();
//        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                showToast("discoverPeers success");
//            }
//            @Override
//            public void onFailure(int error) {}
//        });
//        mDisposables.add(Observable.just(mWifiP2pManager)
//                .delay(SERVICE_BROADCASTING_INTERVAL, TimeUnit.SECONDS)
//                .repeat()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<WifiP2pManager>() {
//                    @Override
//                    public void accept(WifiP2pManager wifiP2pManager) throws Exception {
//                        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
//                            @Override
//                            public void onSuccess() {
//                                showToast("discoverPeers success");
//                            }
//
//                            @Override
//                            public void onFailure(int error) {
//                            }
//                        });
//                    }
//                }));
    }

    @Override
    public void setOnConnectionChangeListener(OnConnectionChangeListener listener) {
    }

    /**
     * 初始化WifiP2p类
     */
    private void initWifiP2p() {
        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);
    }

    /**
     * 初始化BroadcastReceiver
     */
    private void initReceiver() {
        mWifiDirectReceiver = new WifiDirectReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mWifiDirectReceiver.setOnWifiDirectStateChangeListener(this);
    }

    @Override
    public void onWifiDirectStateChange(boolean enable) {
        showToast("onWifiDirectStateChange");
    }

    @Override
    public void onPeersChange() {
    }


    @Override
    public void onConnectionChange(WifiP2pInfo wifiP2pInfo, NetworkInfo networkInfo) {
        if (wifiP2pInfo.groupOwnerAddress != null) {
            showToast("onConnect" + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        }
    }

    @Override
    public void onDeviceDetailChange() {
    }

    /**
     * 绑定生命周期
     */
    private void setupShadowFragment(AppCompatActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        ShadowFragment current = getShadowFragment(manager);
        current.getLifecycle().addListener(this);
    }


    /**
     * 设置生命周期关联，绑定传入的activity的生命周期
     */
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
    public void onResume() {
        mContext.registerReceiver(mWifiDirectReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        mContext.unregisterReceiver(mWifiDirectReceiver);
    }

    @Override
    public void onDestroy() {
        mContext = null;
        mWifiP2pManager.stopPeerDiscovery(mChannel, null);
        mWifiP2pManager.cancelConnect(mChannel, null);
        mWifiP2pManager.clearLocalServices(mChannel, null);
        mWifiP2pManager.clearServiceRequests(mChannel, null);
        if (mDisposables.size() != 0) {
            mDisposables.dispose();
        }
    }


    private void showToast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }


    private static class RxActionListener implements WifiP2pManager.ActionListener {
        private PublishSubject<Integer> mSubject;
        private final int mType;

        RxActionListener(PublishSubject<Integer> subject, int type) {
            mSubject = subject;
            mType = type;
        }

        @Override
        public void onSuccess() {
            if (mSubject != null) {
                mSubject.onNext(mType);
            }
        }

        @Override
        public void onFailure(int reason) {
            mSubject.onError(new RuntimeException("type = " + mType + " reason = " + reason));
        }
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

