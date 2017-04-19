package com.leaves.app.shareme.ui.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.eventbus.RxBus;
import com.leaves.app.shareme.eventbus.TimeSeekEvent;
import com.leaves.app.shareme.service.MusicServerService;

import net.majorkernelpanic.streaming.PlaytimeProvider;
import net.majorkernelpanic.streaming.ReceiveSession;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;

public class MusicPlayerFragment extends BottomSheetFragment implements RtspClient.Callback, ReceiveSession.Callback, PlaytimeProvider {

    @BindView(R.id.iv_cover)
    ImageView mCoverView;

    @BindView(R.id.tv_title)
    TextView mTitleView;

    @BindView(R.id.tv_sub_title)
    TextView mSubTextView;
    private RtspClient mClient;
    private ReceiveSession mSession;
    private Media mPlayingAudio;
    private long mCurrentPlayTime;

//    private OnFragmentInteractionListener mListener;

    public MusicPlayerFragment() {
        // Required empty public constructor
    }

    public static MusicPlayerFragment newInstance() {
        MusicPlayerFragment fragment = new MusicPlayerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RxBus.getDefault().toFlowable(TimeSeekEvent.class)
                .subscribe(new Consumer<TimeSeekEvent>() {
                    @Override
                    public void accept(TimeSeekEvent timeSeekEvent) throws Exception {
                        mCurrentPlayTime = timeSeekEvent.getCurrentTime();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_music_player;
    }

    @OnClick(R.id.bt_play)
    public void playAsClient() {

    }

    public void playAsServer(Media media) {
        if (media != null) {
            Glide.with(this).load(media.getImage())
                    .asBitmap().into(mCoverView);
            mTitleView.setText(media.getTitle());
            mSubTextView.setText(media.getArtist());
            mPlayingAudio = media;
        }
    }

    @Override
    public void onBitrateUpdate(long bitrate) {

    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
    }

    @Override
    public void onSessionStarted() {

    }

    @Override
    public void onSessionStopped() {

    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {

    }

    @Override
    public long getCurrentPlayTime() {
        return mCurrentPlayTime;
    }

//
//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     * <p>
//     * See the Android Training lesson <a href=
//     * "http://developer.android.com/training/basics/fragments/communicating.html"
//     * >Communicating with Other Fragments</a> for more information.
//     */
//    public interface OnFragmentInteractionListener {
//        void onFragmentInteraction(Uri uri);
//    }
}
