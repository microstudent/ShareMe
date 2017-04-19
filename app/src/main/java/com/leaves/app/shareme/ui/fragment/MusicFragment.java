package com.leaves.app.shareme.ui.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.service.AbsMusicServiceBinder;
import com.leaves.app.shareme.service.MusicClientService;
import com.leaves.app.shareme.service.MusicServerService;
import com.leaves.app.shareme.util.GlideCircleTransform;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MusicFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MusicFragment extends Fragment {
    public static final String TAG = "MusicFragment";
    public static final String IS_SERVER = "is_server";

    @BindView(R.id.iv_cover)
    ImageView mCoverView;

    @BindView(R.id.tv_title)
    TextView mTitleView;

    @BindView(R.id.tv_author)
    TextView mAuthorView;

    private Media mMedia;
    private boolean isServer;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (AbsMusicServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder = null;
        }
    };

    private AbsMusicServiceBinder mBinder;

    public MusicFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static MusicFragment newInstance(boolean isServer) {
        MusicFragment fragment = new MusicFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_SERVER, isServer);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            isServer = bundle.getBoolean(IS_SERVER);
        }
        bindServer();
    }

    private void bindServer() {
        if (getActivity() != null) {
            if (isServer) {
                Intent intent = new Intent(getContext(), MusicServerService.class);
                getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            } else {
                Intent intent = new Intent(getContext(), MusicClientService.class);
                getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_music, container, false);
        ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {
        Glide.with(this).load(R.drawable.bg_piano).asBitmap().transform(new GlideCircleTransform(getContext())).into(mCoverView);
    }
}
