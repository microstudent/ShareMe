package com.leaves.app.shareme.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leaves.app.shareme.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BehaviorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BehaviorFragment extends Fragment {
    public static final String TAG = "BehaviorFragment";

    private OnBehaviorClickListener mListener;

    @BindView(R.id.bt_list)
    View mListView;

    @BindView(R.id.bt_connection)
    View mConnectionView;

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
//        if (getArguments() != null) {
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_behavior, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.bt_list)
    public void onListClick() {
        if (mListener != null) {
            mListener.onBehaviorClick(R.id.bt_list, mListView);
        }
    }

    @OnClick(R.id.bt_connection)
    public void onConnectionClick() {
        if (mListener != null) {
            mListener.onBehaviorClick(R.id.bt_connection, mConnectionView);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnBehaviorClickListener) {
            mListener = (OnBehaviorClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnBehaviorClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnBehaviorClickListener{
        void onBehaviorClick(@IdRes int id, View view);
    }
}
