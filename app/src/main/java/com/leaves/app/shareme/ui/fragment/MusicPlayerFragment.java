package com.leaves.app.shareme.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.eventbus.MediaEvent;
import com.leaves.app.shareme.eventbus.RxBus;
import com.leaves.app.shareme.eventbus.TimeSeekEvent;
import com.leaves.app.shareme.service.MusicService;
import com.leaves.app.shareme.ui.activity.MainActivity;

import net.majorkernelpanic.streaming.PlaytimeProvider;
import net.majorkernelpanic.streaming.ReceiveSession;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static String mServerIp;
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
        Intent intent = new Intent(getContext(), MusicService.class);
        getActivity().startService(intent);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(7236));
        editor.apply();

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
    public void connectionToServer() {
        // Configures the SessionBuilder
        mSession = new ReceiveSession.Builder()
                .setAudioDecoder(ReceiveSession.Builder.AUDIO_AAC)
                .setVideoDecoder(ReceiveSession.Builder.VIDEO_NONE)
                .setCallback(this)
                .build();
        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);
        toggleStream();
    }

    // Connects/disconnects to the RTSP server and starts/stops the stream
    public void toggleStream() {
        if (!mClient.isStreaming()) {
            String ip,port,path;

            // We parse the URI written in the Editext
//            Pattern uri = Pattern.compile("rtsp://(.+):(\\d*)/(.+)");
//            Matcher m = uri.matcher("rtsp://" + mServerIp + ":7236/");
//            m.find();
//            ip = m.group(1);
//            port = m.group(2);
//            path = m.group(3);
            ip = mServerIp;
            port = "7236";
            path = "";
            mClient.setServerAddress(ip, Integer.parseInt(port));
            mClient.setStreamPath(path);
            mClient.startStream();

        } else {
            // Stops the stream and disconnects from the RTSP server
            mClient.stopStream();
        }
    }

    public void setServerIp(String serverIp) {
        mServerIp = serverIp;
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
    public void onAudioAvailable(byte[] data, int relatedTime) {

    }

    @Override
    public void onCurrentTimeUpdate(int currentTime) {

    }

    @Override
    public void onRtspUpdate(int message, Exception exception) {

    }

    public void play(Media media) {
        if (media != null) {
            Glide.with(this).load(media.getImage())
                    .asBitmap().into(mCoverView);
            mTitleView.setText(media.getTitle());
            mSubTextView.setText(media.getArtist());
            mPlayingAudio = media;
            MediaEvent event = new MediaEvent(MusicService.ACTION_PLAY, media);
            RxBus.getDefault().post(event);
            // Configures the SessionBuilder
            SessionBuilder.getInstance()
                    .setContext(getContext().getApplicationContext())
                    .setVideoEncoder(SessionBuilder.VIDEO_NONE)
                    .setMp3Path(media.getSrc())
                    .setPlaytimeProvider(this)
                    .setAudioEncoder(SessionBuilder.AUDIO_MP3);

            // Starts the RTSP server
            getActivity().startService(new Intent(getContext(), RtspServer.class));
        }
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
