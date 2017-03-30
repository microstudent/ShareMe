package com.leaves.sdk.wifidirect;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.*;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.leaves.sdk.wifidirect.listener.LifecycleListener;
import com.leaves.sdk.wifidirect.listener.OnConnectionChangeListener;
import com.leaves.sdk.wifidirect.listener.OnDeviceDetailChangeListener;
import com.leaves.sdk.wifidirect.listener.OnServiceFoundListener;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.BooleanSupplier;
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

    private static final long SERVICE_DISCOVERING_INTERVAL = 10;//sec

    private IntentFilter mIntentFilter;
    private Context mContext;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WifiDirectReceiver mWifiDirectReceiver;
    private CompositeDisposable mDisposables;

    private String mInstanceName, mServiceName;

    private WifiP2pDnsSdServiceRequest mWifiP2pServiceRequest;
    private PublishSubject<Integer> mSetSignSubject;
    private PublishSubject<Integer> mDiscoverSubject;



    private boolean isServiceDiscovered = false;

    private String mTimeStamp;
    private String mKey;

    private OnServiceFoundListener mOnServiceFoundListener;
    private DnsSdServiceResponseHandler mDnsSdServiceResponseHandler;

    private OnConnectionChangeListener mOnConnectionChangeListener;
    private WifiP2pInfo mWifiP2pInfo;
    private WifiP2pDevice mThisDevice;

    private OnDeviceDetailChangeListener mOnDeviceDetailChangeListener;

    public WifiDirect(AppCompatActivity rootActivity, String instanceName, String serviceName) {
        this(rootActivity.getSupportFragmentManager(), rootActivity.getApplicationContext(), instanceName, serviceName);
    }

    public WifiDirect(FragmentManager manager, Context context, String instanceName, String serviceName) {
        mContext = context.getApplicationContext();
        initWifiP2p();
        initReceiver();
        setupShadowFragment(manager);//绑定生命周期
        mDisposables = new CompositeDisposable();
        mInstanceName = instanceName;
        mServiceName = serviceName;
    }

    public void discover() {
        setupServiceDiscovery();
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
//                        showToast("discovering services");
                        mWifiP2pManager.discoverServices(mChannel, new RxActionListener(mDiscoverSubject, TYPE_DISCOVER_SERVICES));
                        mDisposables.add(mDiscoverSubject.delay(SERVICE_DISCOVERING_INTERVAL, TimeUnit.SECONDS).repeatUntil(new BooleanSupplier() {
                            @Override
                            public boolean getAsBoolean() throws Exception {
                                return isServiceDiscovered;
                            }
                        }).subscribe(new Consumer<Integer>() {
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
//                showToast(throwable.toString());
            }
        });
        mDiscoverSubject.onNext(0);
    }

    private void setupServiceDiscovery() {
        if (mDnsSdServiceResponseHandler == null) {
            mDnsSdServiceResponseHandler = new DnsSdServiceResponseHandler(mOnServiceFoundListener);
        }
        mWifiP2pManager.setDnsSdResponseListeners(mChannel, mDnsSdServiceResponseHandler, mDnsSdServiceResponseHandler);
        mWifiP2pServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
    }

    @Override
    public void setupSignAndScan(final Map<String, String> params) {
        mSetSignSubject = PublishSubject.create();
        mSetSignSubject.subscribe(new Consumer<Integer>() {
            static final int TYPE_CLEAR_LOCAL_SERVICES = 1;
            static final int TYPE_ADD_LOCAL_SERVICE = 2;

            @Override
            public void accept(Integer integer) throws Exception {
                switch (integer) {
                    case TYPE_CLEAR_LOCAL_SERVICES:
                        WifiP2pDnsSdServiceInfo info = WifiP2pDnsSdServiceInfo.newInstance(mInstanceName, mServiceName, params);
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
//                showToast(throwable.toString());
            }
        });
        mSetSignSubject.onNext(0);
    }

    @Override
    public void stopDiscover() {
        isServiceDiscovered = true;
        mDisposables.dispose();
    }

    @Override
    public void connectTo(WifiP2pDevice device, int groupOwnerIntent) {
        if (device != null) {
            if (mChannel == null) {
                throw new IllegalStateException("you must call setupSign first!");
            }
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.groupOwnerIntent = groupOwnerIntent;
            mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {}
                @Override
                public void onFailure(int reason) {
//                    showToast("onConnectFail,reason:" + reason);
                }
            });
        }
    }

    /**
     * 启动SERVICE广播
     */
    private void startServiceBroadcasting() {
        discover();
    }

    @Override
    public void setOnConnectionChangeListener(OnConnectionChangeListener listener) {
        mOnConnectionChangeListener = listener;
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
//        showToast("onWifiDirectStateChange");
    }

    @Override
    public void onPeersChange() {
    }


    @Override
    public void onConnectionChange(WifiP2pInfo wifiP2pInfo, NetworkInfo networkInfo) {
        //使用requestPeer来获取设备列表
        if (mWifiP2pManager != null && mChannel != null) {
            mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    if (mOnConnectionChangeListener != null) {
                        mOnConnectionChangeListener.onConnectionChange(peers);
                    }
                }
            });
        }
        mWifiP2pInfo = wifiP2pInfo;
    }

    public String getDeviceName() {
        if (mThisDevice != null) {
            return mThisDevice.deviceName;
        }
        return "";
    }

    public boolean isGroupOwner() {
        return mWifiP2pInfo != null && mWifiP2pInfo.isGroupOwner;
    }

    @Nullable
    public String getGroupOwnerIp() {
        if (mWifiP2pInfo != null && mWifiP2pInfo.groupOwnerAddress != null) {
            return mWifiP2pInfo.groupOwnerAddress.getHostAddress();
        }
        return null;
    }

    @Override
    public void onDeviceDetailChange(WifiP2pDevice device) {
        mThisDevice = device;
        if (mOnDeviceDetailChangeListener != null) {
            mOnDeviceDetailChangeListener.onDeviceDetailChanged(device);
        }
    }

    public void setOnDeviceDetailChangeListener(OnDeviceDetailChangeListener onDeviceDetailChangeListener) {
        mOnDeviceDetailChangeListener = onDeviceDetailChangeListener;
    }

    /**
     * 绑定生命周期
     */
    private void setupShadowFragment(AppCompatActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        ShadowFragment current = getShadowFragment(manager);
        current.getLifecycle().addListener(this);
    }

    private void setupShadowFragment(FragmentManager manager) {
        ShadowFragment current = getShadowFragment(manager);
        current.getLifecycle().addListener(this);
    }


    public void setOnServiceFoundListener(OnServiceFoundListener onServiceFoundListener) {
        mOnServiceFoundListener = onServiceFoundListener;
        if (mDnsSdServiceResponseHandler != null) {
            mDnsSdServiceResponseHandler.setOnServiceFoundListener(mOnServiceFoundListener);
        }
    }

    private class DnsSdServiceResponseHandler implements WifiP2pManager.DnsSdServiceResponseListener, WifiP2pManager.DnsSdTxtRecordListener {
        private HashMap<String, ServiceResponse> mResponseHashMap;
        private OnServiceFoundListener mOnServiceFoundListener;


        public DnsSdServiceResponseHandler() {
            this(null);
        }

        public DnsSdServiceResponseHandler(OnServiceFoundListener listener) {
            mOnServiceFoundListener = listener;
            mResponseHashMap = new HashMap<>();
        }

        public void setOnServiceFoundListener(OnServiceFoundListener onServiceFoundListener) {
            mOnServiceFoundListener = onServiceFoundListener;
        }

        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
            ServiceResponse response = mResponseHashMap.get(srcDevice.deviceAddress);
            if (response != null && mOnServiceFoundListener != null) {
                response.setInstanceName(instanceName);
                response.setRegistrationType(registrationType);
                mOnServiceFoundListener.onServiceFound(srcDevice, response);
            } else {
                response = new ServiceResponse(instanceName, registrationType, srcDevice);
                mResponseHashMap.put(srcDevice.deviceAddress, response);
            }
        }

        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
            ServiceResponse response = mResponseHashMap.get(srcDevice.deviceAddress);
            if (response != null && mOnServiceFoundListener != null) {
                response.setFullDomainName(fullDomainName);
                response.setTxtRecordMap(txtRecordMap);
                mOnServiceFoundListener.onServiceFound(srcDevice, response);
            } else {
                response = new ServiceResponse(fullDomainName, txtRecordMap, srcDevice);
                mResponseHashMap.put(srcDevice.deviceAddress, response);
            }
        }
    }

    public class ServiceResponse {
        public String instanceName,registrationType, fullDomainName;
        public Map<String, String> txtRecordMap;
        public WifiP2pDevice wifiP2pDevice;

        public ServiceResponse(String instanceName, String registrationType, WifiP2pDevice wifiP2pDevice) {
            this.instanceName = instanceName;
            this.registrationType = registrationType;
            this.wifiP2pDevice = wifiP2pDevice;
        }

        public ServiceResponse(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice wifiP2pDevice) {
            this.fullDomainName = fullDomainName;
            this.txtRecordMap = txtRecordMap;
            this.wifiP2pDevice = wifiP2pDevice;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }

        public void setRegistrationType(String registrationType) {
            this.registrationType = registrationType;
        }

        public void setFullDomainName(String fullDomainName) {
            this.fullDomainName = fullDomainName;
        }

        public void setTxtRecordMap(Map<String, String> txtRecordMap) {
            this.txtRecordMap = txtRecordMap;
        }
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

