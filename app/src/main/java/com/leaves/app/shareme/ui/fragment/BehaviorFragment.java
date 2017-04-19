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
 * Use the {@link BehaviorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BehaviorFragment extends Fragment {

//    private OnBehaviorClickListener mListener;

    public BehaviorFragment() {
        // Required empty public constructor
    }

    public static BehaviorFragment newInstance() {
        BehaviorFragment fragment = new BehaviorFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_behavior, container, false);
    }


//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnBehaviorClickListener) {
//            mListener = (OnBehaviorClickListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnBehaviorClickListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    public interface OnBehaviorClickListener{
//
//    }
}
