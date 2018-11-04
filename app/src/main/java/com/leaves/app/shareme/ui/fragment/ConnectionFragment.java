package com.leaves.app.shareme.ui.fragment;

import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leaves.app.shareme.R;
import com.leaves.app.shareme.ui.adapter.DeviceListAdapter;
import com.leaves.sdk.wifidirect.WifiDirect;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.leaves.app.shareme.Constant.WifiDirect.INSTANCE_NAME;
import static com.leaves.app.shareme.Constant.WifiDirect.SERVICE_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectionFragment extends Fragment {
    public static final String DEVICE_LIST = "device_list";
    public static final String TAG = "connectionFragment";
//    private OnFragmentInteractionListener mListener;

    @BindView(R.id.rv)
    RecyclerView mRecyclerView;

    public WifiDirect mWifiDirect;

    private DeviceListAdapter mAdapter;
    private ArrayList<WifiP2pDevice> mDeviceList;

    public ConnectionFragment() {
        // Required empty public constructor
    }

    public static ConnectionFragment newInstance(ArrayList<WifiP2pDevice> devices) {
        ConnectionFragment fragment = new ConnectionFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(DEVICE_LIST, devices);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mDeviceList = bundle.getParcelableArrayList(DEVICE_LIST);
        }
        mWifiDirect = WifiDirect.getInstance(getFragmentManager(), getContext(), INSTANCE_NAME, SERVICE_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connection, container, false);
        ButterKnife.bind(this, view);
        setupView();
        return view;
    }

    private void setupView() {
        if (mDeviceList != null) {
            mAdapter = new DeviceListAdapter(mDeviceList, R.layout.layout_device);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        }
    }

    @OnClick(R.id.bt_disconnect)
    public void disconnect() {
        if (mWifiDirect != null) {
            new AlertDialog.Builder(getContext())
                    .setMessage("你确定要断开连接吗？")
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mWifiDirect.disconnectAll();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        }
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
