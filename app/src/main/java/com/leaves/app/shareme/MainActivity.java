package com.leaves.app.shareme;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.leaves.app.shareme.widget.dialpad.NineKeyDialpad;
import com.leaves.app.shareme.widget.dialpad.listener.OnNumberClickListener;
import com.leaves.app.shareme.wifidirect.WifiDirect;
import com.leaves.app.shareme.wifidirect.listener.OnServiceFoundListener;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.dialpad)
    NineKeyDialpad mNineKeyDialpad;

    @BindView(R.id.tv_key)
    TextView mKeyView;

    @BindView(R.id.tv_info)
    TextView mInfoView;

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
                mInfoView.setText(device.deviceName);
                mWifiDirect.stopDiscover();
            }
        });

        mNineKeyDialpad.setOnNumberClickListener(new OnNumberClickListener() {
            @Override
            public void onNumberClick(String number) {
                if (mKeyView.getText().length() >= 4) {
                    return;
                }
                mKeyView.append(number);
                if (mKeyView.getText().length() >= 4) {
                    Map<String, String> txt = new HashMap<>();
                    txt.put(Constant.WifiDirect.KEY_NUMBER, mKeyView.getText().toString());
                    txt.put(Constant.WifiDirect.KEY_TIMESTAMP, System.currentTimeMillis() + "");

                    mTimeStamp = System.currentTimeMillis() + "";
                    mWifiDirect.setupSignAndScan(txt);
                }
            }
        });
    }
}
