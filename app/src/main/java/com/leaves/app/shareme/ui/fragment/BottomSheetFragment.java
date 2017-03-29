package com.leaves.app.shareme.ui.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leaves.app.shareme.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentMeasureListener} interface
 * to handle interaction events.
 * Use the {@link BottomSheetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public abstract class BottomSheetFragment extends Fragment {
    private OnFragmentMeasureListener mListener;

    public BottomSheetFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(getLayoutId(), container, false);
        return view;
    }

    protected abstract int getLayoutId();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentMeasureListener) {
            mListener = (OnFragmentMeasureListener) context;
            if (getView() != null) {
                mListener.onFragmentMeasure(getView().getMeasuredWidth(), getView().getMeasuredHeight());
            }
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentMeasureListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentMeasureListener {
        void onFragmentMeasure(int width, int height);
    }
}
