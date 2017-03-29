package com.leaves.app.shareme;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.bumptech.glide.Glide;
import com.fivehundredpx.android.blur.BlurringView;
import com.leaves.app.shareme.ui.behavior.DodgeBottomSheetBehavior;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.iv_bg)
    ImageView mImageView;

    @BindView(R.id.blur_toolbar)
    BlurringView mToolbarBlurringView;

    @BindView(R.id.activity_main)
    CoordinatorLayout mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        View bottomSheet = mRootView.findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        ((DodgeBottomSheetBehavior) behavior).setMinOffset(300);

        Glide.with(this).load(R.drawable.bg_piano).into(mImageView);
        mToolbarBlurringView.setBlurredView(mImageView);
        mToolbarBlurringView.setBlurRadius(10);
        mToolbarBlurringView.setDownsampleFactor(10);
        mToolbarBlurringView.setOverlayColor(Color.TRANSPARENT);
    }
}
