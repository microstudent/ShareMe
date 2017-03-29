package com.leaves.app.shareme.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.fivehundredpx.android.blur.BlurringView;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.contract.WifiDirectionContract;
import com.leaves.app.shareme.presenter.WifiDirectionPresenter;
import com.leaves.app.shareme.ui.behavior.DodgeBottomSheetBehavior;
import com.leaves.app.shareme.ui.fragment.BottomSheetFragment;
import com.leaves.app.shareme.ui.fragment.DialpadFragment;
import com.leaves.app.shareme.ui.widget.PasswordTextView;
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnNumberClickListener;


public class MainActivity extends AppCompatActivity implements WifiDirectionContract.View,
        OnNumberClickListener, BottomSheetFragment.OnFragmentMeasureListener {
    private FragmentManager mFragmentManager;

    @BindView(R.id.iv_bg)
    ImageView mImageView;

    @BindView(R.id.blur_toolbar)
    BlurringView mToolbarBlurringView;

    @BindView(R.id.activity_main)
    CoordinatorLayout mRootView;

    @BindView(R.id.tv_key)
    PasswordTextView mPasswordTextView;

    private StringBuilder mPassword;

    private WifiDirectionContract.Presenter mPresenter;

    private DodgeBottomSheetBehavior mBottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();
        setupView();
    }

    private void initView() {
        mFragmentManager = getSupportFragmentManager();
        View bottomSheet = mRootView.findViewById(R.id.bottom_sheet);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        if (behavior instanceof DodgeBottomSheetBehavior) {
            mBottomSheetBehavior = (DodgeBottomSheetBehavior) behavior;
        }
        mPassword = new StringBuilder();
        mPresenter = new WifiDirectionPresenter(this);
    }

    private void setupView() {
        mFragmentManager.beginTransaction().add(R.id.container_bottom, DialpadFragment.newInstance())
                .commit();

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        mBottomSheetBehavior.setMinOffset(300);
        mBottomSheetBehavior.setScrollable(false);

        Glide.with(this).load(R.drawable.bg_piano).into(mImageView);
        mToolbarBlurringView.setBlurredView(mImageView);
        mToolbarBlurringView.setBlurRadius(10);
        mToolbarBlurringView.setDownsampleFactor(10);
        mToolbarBlurringView.setOverlayColor(Color.TRANSPARENT);
    }

    @Override
    public void onFragmentMeasure(int width, int height) {
        if (mBottomSheetBehavior != null) {
            mBottomSheetBehavior.setPeekHeight(height);
        }
    }

    @Override
    public void onNumberClick(String number) {
        if (mPassword.length() >= 4) {
            return;
        }
        mPasswordTextView.append(number);
        mPassword.append(number);
        if (mPassword.length() >= 4) {
            mPresenter.setPassword(mPassword.toString());
        }
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
