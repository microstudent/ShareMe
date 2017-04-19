package com.leaves.app.shareme.ui.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import jp.wasabeef.blurry.Blurry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.contract.MainActivityContract;
import com.leaves.app.shareme.presenter.MainPresenter;
import com.leaves.app.shareme.ui.behavior.DockerBehavior;
import com.leaves.app.shareme.ui.behavior.DodgeBottomSheetBehavior;
import com.leaves.app.shareme.ui.fragment.AudioListFragment;
import com.leaves.app.shareme.ui.fragment.BehaviorFragment;
import com.leaves.app.shareme.ui.fragment.BottomSheetFragment;
import com.leaves.app.shareme.ui.fragment.DialpadFragment;
import com.leaves.app.shareme.ui.fragment.PasswordFragment;
import com.leaves.app.shareme.ui.fragment.MusicPlayerFragment;
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnNumberClickListener;
import com.tbruyelle.rxpermissions2.RxPermissions;

import static android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED;
import static android.support.design.widget.BottomSheetBehavior.STATE_EXPANDED;


public class MainActivity extends AppCompatActivity implements
        OnNumberClickListener, BottomSheetFragment.OnFragmentMeasureListener,
        PasswordFragment.MainFragmentCallback, AudioListFragment.OnAudioClickListener, MainActivityContract.View {

    private FragmentManager mFragmentManager;

    @BindView(R.id.iv_bg)
    ImageView mImageView;

    @BindView(R.id.activity_main)
    CoordinatorLayout mRootView;

    @BindView(R.id.view_content)
    View mContentView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private DodgeBottomSheetBehavior mBottomSheetBehavior;
    private DockerBehavior mDockerBehavior;
    private PasswordFragment mPasswordFragment;
    private MusicPlayerFragment mMusicPlayerFragment;
    private AudioListFragment mAudioListFragment;
    private Fragment mLastBottomFragment;
    private MainActivityContract.Presenter mPresenter;
    private int mMode = -1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        RxPermissions rxPermissions = new RxPermissions(this);

        rxPermissions.request(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            Toast.makeText(getApplicationContext(), "必须赋予存储权限才可以使用", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            initView();
                        }
                    }
                });
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
        mDockerBehavior = DockerBehavior.from(mContentView);
        mPresenter = new MainPresenter(this, getSupportFragmentManager(), this);
    }

    private void setupView() {
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == STATE_EXPANDED) {
                    mDockerBehavior.setNeedMeasure(false);
                    if (mAudioListFragment == null) {
                        mAudioListFragment = AudioListFragment.newInstance();
                    }
                    switchFragment(mAudioListFragment, R.id.bottom_sheet);
                    mBottomSheetBehavior.setScrollable(true);
                } else if (newState == STATE_COLLAPSED) {
                    mDockerBehavior.setNeedMeasure(false);
                    if (mMusicPlayerFragment == null) {
                        mMusicPlayerFragment = MusicPlayerFragment.newInstance();
                    }
                    switchFragment(mMusicPlayerFragment, R.id.bottom_sheet);
                    mBottomSheetBehavior.setScrollable(true);
                }
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

    @Override
    public void setupFragment(int mode) {
        if (mMode != mode) {
            mPasswordFragment = PasswordFragment.newInstance();
            mFragmentManager.beginTransaction()
                    .add(R.id.container_main, mPasswordFragment)
                    .commit();
            Fragment fragment;
            switch (mode) {
                case MainPresenter.MODE_STARTUP:
                    fragment = DialpadFragment.newInstance();
                    switchFragment(fragment, R.id.bottom_sheet);
                    break;
                case MainPresenter.MODE_CONNECTED:
                    fragment = BehaviorFragment.newInstance();
                    switchFragment(fragment, R.id.bottom_sheet);
                    break;
            }
        }
        mMode = mode;
    }

    @Override
    public void onFragmentMeasure(int width, int height) {
        if (mBottomSheetBehavior != null) {
            mBottomSheetBehavior.setPeekHeight(height);
        }
    }

    @Override
    public void onNumberClick(String number) {
        if (mPasswordFragment != null) {
            mPasswordFragment.appendPassword(number);
        }
        mPresenter.appendPassword(number);
    }

    @Override
    public void onSearchingDevice() {
//        mMusicPlayerFragment = BehaviorFragment.newInstance();
        switchFragment(BehaviorFragment.newInstance(), R.id.bottom_sheet);
        mBottomSheetBehavior.setScrollable(true);
    }

    private void switchFragment(Fragment to, @IdRes int resId) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        if (mLastBottomFragment == null) {
            transaction.add(resId, to);
        } else if (to != mLastBottomFragment) {
            if (!to.isAdded()) {
                transaction.hide(mLastBottomFragment)
                        .add(resId, to).addToBackStack(null);
            } else {
                transaction.hide(mLastBottomFragment)
                        .show(to).addToBackStack(null);
            }
        }
        transaction.commit();
        mLastBottomFragment = to;
    }

    public void mock(View view) {
        onSearchingDevice();
    }

    @Override
    public void onAudioClick(Media media) {
        mBottomSheetBehavior.setState(STATE_COLLAPSED);
        mMusicPlayerFragment.playAsServer(media);
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSearching() {
        mPasswordFragment.switchToMode(PasswordFragment.MODE_SEARCHING);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        moveTaskToBack(true);
    }
}
