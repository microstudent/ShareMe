package com.leaves.app.shareme.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import jp.wasabeef.blurry.Blurry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.GlideApp;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.SApplication;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.contract.MainActivityContract;
import com.leaves.app.shareme.presenter.MainPresenter;
import com.leaves.app.shareme.ui.fragment.AudioListFragment;
import com.leaves.app.shareme.ui.fragment.BehaviorFragment;
import com.leaves.app.shareme.ui.fragment.BottomSheetFragment;
import com.leaves.app.shareme.ui.fragment.ConnectionFragment;
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

    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION = "current_media";
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
    private AudioListFragment mAudioListFragment;
    private MainActivityContract.Presenter mPresenter;
    private int mMode = -1;
    private MusicFragment mMusicFragment;
    private ConnectionFragment mConnectionFragment;

    @Override
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
        WifiManager wifiManager = (WifiManager) SApplication.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    private void initView() {
        mFragmentManager = getSupportFragmentManager();
        mPresenter = new MainPresenter(this, getSupportFragmentManager(), this);
    }

    private void setupView() {
        setSupportActionBar(mToolbar);
        setTitle("");
//        mBottomSheetBehavior.setMinOffset(300);

        GlideApp.with(this)
                .asBitmap()
                .load(R.drawable.bg_piano)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        Blurry.with(MainActivity.this).radius(10).sampling(10).async().animate().from(resource).into(mImageView);
                        return false;
                    }
                }).preload();
    }

    @Override
    public void setupFragment(int mode) {
        if (mMode != mode) {
            Fragment fragment;
            switch (mode) {
                default:
                case MainPresenter.MODE_STARTUP:
                    mPasswordFragment = PasswordFragment.newInstance();
                    mFragmentManager.beginTransaction()
                            .replace(R.id.container_main, mPasswordFragment, PasswordFragment.TAG)
                            .commit();
                    fragment = DialpadFragment.newInstance();
                    mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, DialpadFragment.TAG).commit();
//                    switchFragment(BehaviorFragment.TAG, fragment, DialpadFragment.TAG, R.id.container_bottom, false);
                    mPresenter.cancelSearch();
                    break;
                case MainPresenter.MODE_CONNECTED:
                    mMusicFragment = MusicFragment.newInstance(mPresenter.getRole() == Constant.ROLE_SERVER);
                    mFragmentManager.beginTransaction()
                            .replace(R.id.container_main, mMusicFragment, MusicFragment.TAG)
                            .commit();
                    fragment = BehaviorFragment.newInstance();
                    mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, BehaviorFragment.TAG).commit();
//                    switchFragment(DialpadFragment.TAG, fragment, BehaviorFragment.TAG, R.id.container_bottom, false);
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
        Fragment fragment = BehaviorFragment.newInstance();
        mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, BehaviorFragment.TAG).commit();
    }

    @Override
    public void cancelSearch() {
        if (mPresenter != null) {
            mPresenter.cancelSearch();
        }
        Fragment fragment = DialpadFragment.newInstance();
        mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, DialpadFragment.TAG).commit();
//        switchFragment(BehaviorFragment.TAG, DialpadFragment.newInstance(), DialpadFragment.TAG, R.id.container_bottom, false);
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

    @Override
    public void onAudioClick(Media media) {
        mMusicFragment.play(media);
        mAudioListFragment.dismiss();
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
            case R.id.action_setting:
                Intent intent = new Intent(this, SettingActivity.class);
                intent.putExtra(Constant.TAG_ROLE_TYPE, mPresenter.getRole());
                startActivity(intent);
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
            if (mMusicFragment != null && mMusicFragment.isConnectionAlive()) {
                if (mAudioListFragment == null) {
                    mAudioListFragment = AudioListFragment.newInstance(mPresenter.getRole() == Constant.ROLE_SERVER);
                }
                switchFragment(AudioListFragment.TAG, mAudioListFragment, AudioListFragment.TAG, 0, false);
            } else {
                showToast("未能连接服务端/客户端");
            }
        } else if (id == R.id.bt_connection) {
            if (mMusicFragment != null && mMusicFragment.isConnectionAlive()) {
                if (mConnectionFragment == null) {
                    mConnectionFragment = ConnectionFragment.newInstance(mPresenter.getDeviceList());
                }
                if (!mConnectionFragment.isAdded() || mConnectionFragment.isDetached()) {
                    switchFragment(MusicFragment.TAG, mConnectionFragment, ConnectionFragment.TAG, R.id.container_main, true);
                }
            } else {
                showToast("未能连接服务端/客户端");
            }
        }
    }
}
