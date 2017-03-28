package com.leaves.app.shareme;

import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.fivehundredpx.android.blur.BlurringView;
import com.leaves.app.shareme.widget.PasswordTextView;
import com.leaves.app.shareme.widget.dialpad.NineKeyDialpad;
import com.leaves.app.shareme.widget.dialpad.listener.OnNumberClickListener;
import com.leaves.app.shareme.wifidirect.WifiDirect;
import com.leaves.app.shareme.wifidirect.listener.OnDeviceDetailChangeListener;
import com.leaves.app.shareme.wifidirect.listener.OnServiceFoundListener;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.dialpad)
    NineKeyDialpad mNineKeyDialpad;

    @BindView(R.id.tv_key)
    PasswordTextView mKeyView;

    @BindView(R.id.iv_bg)
    ImageView mImageView;

    @BindView(R.id.activity_main)
    ViewGroup mRootView;

    @BindView(R.id.blur_toolbar)
    BlurringView mToolbarBlurringView;

    private WifiDirect mWifiDirect;

    private String mTimeStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mWifiDirect = new WifiDirect(this, Constant.WifiDirect.INSTANCE_NAME, Constant.WifiDirect.SERVICE_TYPE);

        mWifiDirect.setOnServiceFoundListener(new OnServiceFoundListener() {
            @Override
            public void onServiceFound(WifiP2pDevice device, WifiDirect.ServiceResponse response) {
                mWifiDirect.stopDiscover();
                mWifiDirect.connectTo(device, -1);
            }
        });

        mNineKeyDialpad.setOnNumberClickListener(new OnNumberClickListener() {
            @Override
            public void onNumberClick(String number) {
                if (mKeyView.getPassword().length() >= 4) {
                    return;
                }
                mKeyView.append(number);
                if (mKeyView.getPassword().length() >= 4) {
                    Map<String, String> txt = new HashMap<>();
                    txt.put(Constant.WifiDirect.KEY_NUMBER, mKeyView.getPassword());
                    txt.put(Constant.WifiDirect.KEY_TIMESTAMP, System.currentTimeMillis() + "");

                    mTimeStamp = System.currentTimeMillis() + "";
                    mWifiDirect.setupSignAndScan(txt);
                }
            }
        });
        Glide.with(this).load(R.drawable.bg_piano).into(mImageView);
        mToolbarBlurringView.setBlurredView(mImageView);
        mToolbarBlurringView.setBlurRadius(10);
        mToolbarBlurringView.setDownsampleFactor(10);
        mToolbarBlurringView.setOverlayColor(Color.TRANSPARENT);
    }

    public void startService(View view) {
        if (mWifiDirect.isGroupOwner()) {
            Intent intent = new Intent(this, ServerActivity.class);
            startActivity(intent);
        }
    }

    public void client(View view) {
        if (!mWifiDirect.isGroupOwner()) {
            Intent intent = new Intent(this, ClientActivity.class);
            intent.putExtra("ip", mWifiDirect.getGroupOwnerIp());
            startActivity(intent);
        }
    }
}
