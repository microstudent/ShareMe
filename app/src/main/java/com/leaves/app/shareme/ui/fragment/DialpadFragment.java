package com.leaves.app.shareme.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leaves.app.shareme.R;
import com.leaves.app.shareme.ui.widget.dialpad.NineKeyDialpad;
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnNumberClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Leaves on 2017/3/29.
 */

public class DialpadFragment extends BottomSheetFragment {
    public static final String TAG = "DialpadFragment";

    private OnNumberClickListener mListener;

    @BindView(R.id.dialpad)
    NineKeyDialpad mNineKeyDialpad;

    public DialpadFragment() {
    }

    public static DialpadFragment newInstance() {
        return new DialpadFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dialpad;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            ButterKnife.bind(this, view);
            if (mListener != null) {
                mNineKeyDialpad.setOnNumberClickListener(mListener);
            }
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnNumberClickListener) {
            mListener = (OnNumberClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNumberClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
