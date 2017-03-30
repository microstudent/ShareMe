package com.leaves.app.shareme.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.leaves.app.shareme.R;
import com.leaves.app.shareme.contract.WifiDirectionContract;
import com.leaves.app.shareme.presenter.WifiDirectionPresenter;
import com.leaves.app.shareme.ui.widget.PasswordTextView;
import com.leaves.app.shareme.ui.widget.dialpad.colorfulanimview.ColorfulAnimView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainFragment extends Fragment implements WifiDirectionContract.View{
    private static final long DEFAULT_ANIM_DURATION = 500;
    private WifiDirectionPresenter mPresenter;


    //    private OnFragmentInteractionListener mListener;
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
        mProgressLayout.setVisibility(View.VISIBLE);
        mProgressLayout.animate().alpha(1).setDuration(DEFAULT_ANIM_DURATION).start();
        mColorfulAnimView.startAnim();
        mPasswordTextView.animate().alpha(0).setDuration(DEFAULT_ANIM_DURATION).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mPasswordTextView.setVisibility(View.GONE);
            }
        }).start();
        mHintView.animate().alpha(0).setDuration(DEFAULT_ANIM_DURATION).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mPasswordTextView.setVisibility(View.GONE);
            }
        }).start();
    }

    @Override
    public void onDeviceFound() {
        mColorfulAnimView.setVisibility(View.GONE);
        mColorfulAnimView.stopAnim();
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
//    public interface OnFragmentInteractionListener {
//        void onFragmentInteraction(Uri uri);
//    }
}
