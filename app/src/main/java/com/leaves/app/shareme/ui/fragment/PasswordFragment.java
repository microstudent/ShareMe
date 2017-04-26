package com.leaves.app.shareme.ui.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.leaves.app.shareme.R;
import com.leaves.app.shareme.ui.widget.PasswordTextView;
import com.leaves.app.shareme.ui.widget.dialpad.colorfulanimview.ColorfulAnimView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PasswordFragment extends Fragment {
    public static final String TAG = "PasswordFragment";
    private static final long DEFAULT_ANIM_DURATION = 500;
    public static final int MODE_PASSWORD = 0;
    public static final int MODE_SEARCHING = 1;

    @IntDef({MODE_SEARCHING, MODE_PASSWORD})
    @Retention(RetentionPolicy.SOURCE)
    @interface Mode {
    }

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

    public PasswordFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PasswordFragment.
     */
    public static PasswordFragment newInstance() {
        PasswordFragment fragment = new PasswordFragment();
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
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }


    public void onDialpadClick(int position, String msg) {
        if (position == 9) {
            //clearPassword all
            mPasswordTextView.clear();
        } else if (position == 11) {
            //backspace
            mPasswordTextView.backspace();
        } else {
            mPasswordTextView.append(msg);
        }
    }

    /**
     * 这个方法只针对UI
     */
    public void switchToMode(@Mode int mode) {
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
