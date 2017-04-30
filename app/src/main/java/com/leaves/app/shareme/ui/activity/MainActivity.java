package com.leaves.app.shareme.ui.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import jp.wasabeef.blurry.Blurry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.contract.MainActivityContract;
import com.leaves.app.shareme.presenter.MainPresenter;
import com.leaves.app.shareme.ui.fragment.AudioListFragment;
import com.leaves.app.shareme.ui.fragment.BehaviorFragment;
import com.leaves.app.shareme.ui.fragment.BottomSheetFragment;
import com.leaves.app.shareme.ui.fragment.DialpadFragment;
import com.leaves.app.shareme.ui.fragment.MusicFragment;
import com.leaves.app.shareme.ui.fragment.PasswordFragment;
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnNumberClickListener;
import com.tbruyelle.rxpermissions2.RxPermissions;


public class MainActivity extends AppCompatActivity implements
        OnNumberClickListener, BottomSheetFragment.OnFragmentMeasureListener,
        PasswordFragment.MainFragmentCallback, AudioListFragment.OnAudioClickListener,
        BehaviorFragment.OnBehaviorClickListener,
        MainActivityContract.View {

    private FragmentManager mFragmentManager;

    @BindView(R.id.iv_bg)
    ImageView mImageView;

    @BindView(R.id.activity_main)
    ViewGroup mRootView;

    @BindView(R.id.view_content)
    View mContentView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private PasswordFragment mPasswordFragment;
    private BehaviorFragment mBehaviorFragment;
    private AudioListFragment mAudioListFragment;
    private MainActivityContract.Presenter mPresenter;
    private int mMode = -1;
    private MusicFragment mMusicFragment;

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
                            setupView();
                        }
                    }
                });
    }

    private void initView() {
        mFragmentManager = getSupportFragmentManager();
        mPresenter = new MainPresenter(this, getSupportFragmentManager(), this);
    }

    private void setupView() {
//        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
//            @Override
//            public void onStateChanged(@NonNull View bottomSheet, int newState) {
//                if (newState == STATE_EXPANDED) {
////                    mDockerBehavior.setNeedMeasure(false);
//                    if (mAudioListFragment == null) {
//                        mAudioListFragment = AudioListFragment.newInstance();
//                    }
//                    switchFragment(BehaviorFragment.TAG, mAudioListFragment, AudioListFragment.TAG, R.id.bottom_sheet);
//                    mBottomSheetBehavior.setScrollable(true);
//                } else if (newState == STATE_COLLAPSED) {
////                    mDockerBehavior.setNeedMeasure(false);
//                    if (mBehaviorFragment == null) {
//                        mBehaviorFragment = BehaviorFragment.newInstance();
//                    }
//                    switchFragment(AudioListFragment.TAG, mBehaviorFragment, BehaviorFragment.TAG, R.id.bottom_sheet);
//                    mBottomSheetBehavior.setScrollable(true);
//                }
//            }
//
//            @Override
//            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//
//            }
//        });
        setSupportActionBar(mToolbar);
        setTitle("");
//        mBottomSheetBehavior.setMinOffset(300);

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
            Fragment fragment;
            switch (mode) {
                case MainPresenter.MODE_STARTUP:
                    mPasswordFragment = PasswordFragment.newInstance();
                    mFragmentManager.beginTransaction()
                            .replace(R.id.container_main, mPasswordFragment, PasswordFragment.TAG)
                            .commit();
                    fragment = DialpadFragment.newInstance();
                    switchFragment(null, fragment, DialpadFragment.TAG, R.id.container_bottom, false);
                    break;
                case MainPresenter.MODE_CONNECTED:
                    mMusicFragment = MusicFragment.newInstance(mPresenter.isServer());
                    mFragmentManager.beginTransaction()
                            .replace(R.id.container_main, mMusicFragment, MusicFragment.TAG)
                            .commit();
                    fragment = BehaviorFragment.newInstance();
                    switchFragment(DialpadFragment.TAG, fragment, BehaviorFragment.TAG, R.id.container_bottom, false);
                    break;
            }
        }
        mMode = mode;
    }

    @Override
    public void onFragmentMeasure(int width, int height) {
    }

    @Override
    public void onDialpadClick(int position, String number) {
        if (mPasswordFragment != null) {
            mPasswordFragment.onDialpadClick(position, number);
        }
        mPresenter.onDialpadClick(position, number);
    }

    /**
     * 由password通知的
     */
    @Override
    public void onSearchingDevice() {
        switchFragment(DialpadFragment.TAG, BehaviorFragment.newInstance(), BehaviorFragment.TAG, R.id.container_bottom, false);
    }

    @Override
    public void cancelSearch() {
        if (mPresenter != null) {
            mPresenter.cancelSearch();
        }
        switchFragment(BehaviorFragment.TAG, DialpadFragment.newInstance(), DialpadFragment.TAG, R.id.container_bottom, false);
    }

    private void switchFragment(String fromTag, Fragment to, String toTag, @IdRes int resId, boolean addToBackStack) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        Fragment fromFragment = mFragmentManager.findFragmentByTag(fromTag);
        if (fromFragment == null) {
            transaction.add(resId, to, toTag);
        } else if (to != fromFragment) {
            if (!to.isAdded()) {
                transaction.hide(fromFragment)
                        .add(resId, to, toTag);
                if (addToBackStack) {
                    transaction.addToBackStack(null);
                }
            } else {
                transaction.hide(fromFragment)
                        .show(to);
                if (addToBackStack) {
                    transaction.addToBackStack(null);
                }
            }
        }
        transaction.commit();
    }

    public void mock(View view) {
        onSearchingDevice();
    }

    @Override
    public void onAudioClick(Media media) {
        mMusicFragment.play(media);
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_exit:
                System.exit(0);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    @Override
    public void onBehaviorClick(@IdRes int id, View view) {
        if (id == R.id.bt_list) {
            if (mMusicFragment != null) {
                if (mMusicFragment.isConnectionAlive()) {
                    if (mAudioListFragment == null) {
                        mAudioListFragment = AudioListFragment.newInstance();
                    }
                    switchFragment(AudioListFragment.TAG, mAudioListFragment, AudioListFragment.TAG, 0, false);
                } else {
                    Toast.makeText(MainActivity.this, "未能连接服务端/客户端", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
