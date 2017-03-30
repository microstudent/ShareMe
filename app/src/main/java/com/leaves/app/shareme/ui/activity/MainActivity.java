package com.leaves.app.shareme.ui.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.blurry.Blurry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.contract.WifiDirectionContract;
import com.leaves.app.shareme.ui.behavior.DodgeBottomSheetBehavior;
import com.leaves.app.shareme.ui.fragment.BottomSheetFragment;
import com.leaves.app.shareme.ui.fragment.DialpadFragment;
import com.leaves.app.shareme.ui.fragment.MainFragment;
import com.leaves.app.shareme.ui.fragment.MusicPlayerFragment;
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnNumberClickListener;


public class MainActivity extends AppCompatActivity implements
        OnNumberClickListener, BottomSheetFragment.OnFragmentMeasureListener,
        MainFragment.MainFragmentCallback {
    private FragmentManager mFragmentManager;

    @BindView(R.id.iv_bg)
    ImageView mImageView;

    @BindView(R.id.activity_main)
    CoordinatorLayout mRootView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private DodgeBottomSheetBehavior mBottomSheetBehavior;
    private MainFragment mMainFragment;

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
    }

    private void setupView() {
        setupFragment();
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

        Glide.with(this).load(R.drawable.bg_piano).asBitmap().listener(new RequestListener<Integer, Bitmap>() {
            @Override
            public boolean onException(Exception e, Integer model, Target<Bitmap> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Integer model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                Blurry.with(MainActivity.this).radius(10).sampling(10).async().animate().from(resource).into(mImageView);
                return true;
            }
        }).preload();
    }

    private void setupFragment() {
        mMainFragment = MainFragment.newInstance();
        mFragmentManager.beginTransaction().add(R.id.container_bottom, DialpadFragment.newInstance())
                .add(R.id.container_main, mMainFragment)
                .commit();
    }

    @Override
    public void onFragmentMeasure(int width, int height) {
        if (mBottomSheetBehavior != null) {
            mBottomSheetBehavior.setPeekHeight(height);
        }
    }

    @Override
    public void onNumberClick(String number) {
        if (mMainFragment != null) {
            mMainFragment.appendPassword(number);
        }
    }

    @Override
    public void onSearchingDevice() {
        mFragmentManager.beginTransaction().replace(R.id.container_bottom, MusicPlayerFragment.newInstance())
                .commit();

    }
}
