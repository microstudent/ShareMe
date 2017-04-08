package com.leaves.app.shareme.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.leaves.app.shareme.R;
import com.leaves.app.shareme.contract.WifiDirectionContract;
import com.leaves.app.shareme.presenter.WifiDirectionPresenter;
import com.leaves.app.shareme.ui.adapter.DeviceListAdapter;
import com.leaves.app.shareme.ui.widget.PasswordTextView;
import com.leaves.app.shareme.ui.widget.dialpad.colorfulanimview.ColorfulAnimView;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainFragment extends Fragment implements WifiDirectionContract.View{
    private static final long DEFAULT_ANIM_DURATION = 500;
    private WifiDirectionContract.Presenter mPresenter;
    public static final int MODE_PASSWORD = 0;
    public static final int MODE_SEARCHING = 1;
    public static final int MODE_DEVICE_LIST = 2;
    private DeviceListAdapter mDeviceAdapter;


    @IntDef({MODE_DEVICE_LIST, MODE_SEARCHING, MODE_PASSWORD})
    @Retention(RetentionPolicy.SOURCE)
    @interface Mode {}

    private MainFragmentCallback mListener;

    @BindView(R.id.tv_key)
    PasswordTextView mPasswordTextView;

    @BindView(R.id.view_anim)
    ColorfulAnimView mColorfulAnimView;

    @BindView(R.id.layout_progress)
    ViewGroup mProgressLayout;

    @BindView(R.id.tv_hint)
    TextView mHintView;

    @BindView(R.id.tv_title)
    TextView mTitleView;

    @BindView(R.id.layout_key)
    ViewGroup mKeyLayout;


    @BindView(R.id.rv)
    RecyclerView mRecyclerView;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MainFragment.
     */
    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPresenter = new WifiDirectionPresenter(this, getFragmentManager(), getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }


    public void appendPassword(String msg) {
        mPresenter.appendPassword(msg);
        mPasswordTextView.append(msg);
    }

    @Override
    public void onStartDiscover() {
        switchToMode(MODE_SEARCHING);
    }

    @Override
    public void onDeviceFound(WifiP2pDevice wifiP2pDevice) {
        switchToMode(MODE_DEVICE_LIST);
        if (mDeviceAdapter == null) {
            initDeviceListView();
        }
        mDeviceAdapter.addNewDevice(wifiP2pDevice);
    }

    @Override
    public void startServer() {
        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(7236));
        editor.apply();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        SessionBuilder.getInstance()
                .setContext(getContext())
                .setVideoEncoder(SessionBuilder.VIDEO_NONE)
                .setMp3Path(path + "/a.mp3")
                .setAudioEncoder(SessionBuilder.AUDIO_MP3).build();

        // Starts the RTSP server
        getActivity().startService(new Intent(getContext(), RtspServer.class));
    }

    @Override
    public void setServerIp() {

    }

    private void initDeviceListView() {
        mDeviceAdapter = new DeviceListAdapter(getContext());
        mRecyclerView.setAdapter(mDeviceAdapter);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
    }

    /**
     * 这个方法只针对UI
     */
    private void switchToMode(@Mode int mode) {
        switch (mode) {
            case MODE_PASSWORD:
                animate(mProgressLayout, false);
                animate(mKeyLayout, true);
                mColorfulAnimView.stopAnim();
                mTitleView.setText(R.string.title_share);
                break;
            case MODE_SEARCHING:
                animate(mProgressLayout, true);
                animate(mKeyLayout, false);
                mColorfulAnimView.startAnim();
                mTitleView.setText(R.string.title_searching);
                mListener.onSearchingDevice();
                break;
            case MODE_DEVICE_LIST:
                mRecyclerView.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void animate(final View view, boolean visibility) {
        if (view != null) {
            if (visibility) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(1);
            } else {
                view.animate().alpha(0).setDuration(DEFAULT_ANIM_DURATION).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }
                }).start();
            }
        }
    }

    @OnClick(R.id.bt_cancel)
    public void cancelSearch() {
        mPresenter.cancelSearch();
        switchToMode(MODE_PASSWORD);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainFragmentCallback) {
            mListener = (MainFragmentCallback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MainFragmentCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface MainFragmentCallback {
        void onSearchingDevice();
    }
}
