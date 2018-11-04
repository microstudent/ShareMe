package com.leaves.app.shareme.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.contract.AudioListContract;
import com.leaves.app.shareme.presenter.AudioListPresenter;
import com.leaves.app.shareme.ui.adapter.AudioListAdapter;
import com.microstudent.app.bouncyfastscroller.RecyclerViewScroller;
import com.microstudent.app.bouncyfastscroller.vertical.VerticalBouncyFastScroller;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AudioListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AudioListFragment extends BottomSheetDialogFragment implements AudioListContract.View{
    public static final String TAG = "AudioListFragment";
    private static final int MIN_DATA_SIZE_FOR_FAST_SCROLLER = 15;
    private static final String IS_SERVER = "is_server";
    private OnAudioClickListener mListener;
    private AudioListAdapter mAdapter;
    @BindView(R.id.vbfs)
    VerticalBouncyFastScroller mVerticalBouncyFastScroller;

    @BindView(R.id.rv)
    RecyclerView mRecyclerView;
    private AudioListPresenter mPresenter;
    private boolean isFirstRun = true;
    private List<Media> mMedia;
    private boolean isFastScrollEnable = false;
    private boolean isServer;

    public AudioListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AudioListFragment.
     */
    public static AudioListFragment newInstance(boolean isServer) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(IS_SERVER, isServer);
        AudioListFragment fragment = new AudioListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_media_list, container, false);
        ButterKnife.bind(this, view);
        if (isFirstRun) {
            mAdapter = new AudioListAdapter();
            mAdapter.setOnAudioClickListener(mListener);
            isServer = getArguments().getBoolean(IS_SERVER, false);
            if (isServer) {
                mPresenter = new AudioListPresenter(this, getContext().getApplicationContext());
                mPresenter.start();
            } else {
                setData(Constant.sPlayList);
            }
            isFirstRun = false;
        }
        setupFastScroll();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setData(List<Media> medias) {
        mAdapter.setData(medias);
        mMedia = medias;
        if (medias.size() > MIN_DATA_SIZE_FOR_FAST_SCROLLER) {
            isFastScrollEnable = true;
            setupFastScroll();
        } else {
            isFastScrollEnable = false;
        }
    }

    private void setupFastScroll() {
        if (isFastScrollEnable && mMedia != null) {
            mVerticalBouncyFastScroller.setEnabled(true);
            mVerticalBouncyFastScroller.setData(mMedia);
            mVerticalBouncyFastScroller.setRecyclerView(mRecyclerView, RecyclerViewScroller.SHOW_INDEX_IN_NEED);
        } else {
            mVerticalBouncyFastScroller.setEnabled(false);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAudioClickListener) {
            mListener = (OnAudioClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnAudioClickListener {
        void onAudioClick(Media media);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.onDestroy();
        }
    }
}
