package com.leaves.app.shareme.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import com.leaves.app.shareme.Constant;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.service.AbsMusicServiceBinder;
import com.leaves.app.shareme.service.MusicClientService;
import com.leaves.app.shareme.service.MusicServerService;
import com.xw.repo.BubbleSeekBar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingActivity extends AppCompatActivity {

    @BindView(R.id.seekbar_left)
    BubbleSeekBar mLeftSeekBar;

    @BindView(R.id.seekbar_right)
    BubbleSeekBar mRightSeekBar;

    private AbsMusicServiceBinder mBinder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (AbsMusicServiceBinder) service;
            int leftVolume = mBinder.getLeftVolume();
            int rightVolume = mBinder.getRightVolume();
            saveVolume(leftVolume, rightVolume);
            setVolume(leftVolume, rightVolume);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder = null;
        }
    };

    private void setVolume(int leftVolume, int rightVolume) {
        if (leftVolume >= 0 && leftVolume <= 100) {
            mLeftSeekBar.setProgress(leftVolume);
        }
        if (rightVolume >= 0 && rightVolume <= 100) {
            mRightSeekBar.setProgress(rightVolume);
        }
    }

    private BubbleSeekBar.OnProgressChangedListener mLeftSeekListener = new BubbleSeekBar.OnProgressChangedListenerAdapter() {
        @Override
        public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
            if (mBinder != null) {
                mBinder.setVolume(progress, -1);
            }
            saveVolume(progress, -1);
        }
    };
    private BubbleSeekBar.OnProgressChangedListener mRightSeekListener = new BubbleSeekBar.OnProgressChangedListenerAdapter() {
        @Override
        public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
            if (mBinder != null) {
                mBinder.setVolume(-1, progress);
            }
            saveVolume(-1, progress);
        }
    };
    private SharedPreferences mSharedPreferences;

    private void saveVolume(int leftVolume, int rightVolume) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        if (leftVolume != -1) {
            editor.putInt(Constant.TAG_LEFT_VOLUME, leftVolume);
        }
        if (rightVolume != -1){
            editor.putInt(Constant.TAG_RIGHT_VOLUME, rightVolume);
        }
        editor.apply();
    }

    private int mRole = Constant.ROLE_UNDEFINED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.setting);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mSharedPreferences = getSharedPreferences(Constant.SP_VOLUME, MODE_PRIVATE);
        setupView();
    }

    private void setupView() {
        mLeftSeekBar.setOnProgressChangedListener(mLeftSeekListener);
        mRightSeekBar.setOnProgressChangedListener(mRightSeekListener);
        Intent intent = getIntent();
        if (intent != null) {
            mRole = intent.getIntExtra(Constant.TAG_ROLE_TYPE, Constant.ROLE_UNDEFINED);
        }
        if (mRole == Constant.ROLE_CLIENT) {
            intent = new Intent(this, MusicClientService.class);
            bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        } else if (mRole == Constant.ROLE_SERVER) {
            intent = new Intent(this, MusicServerService.class);
            bindService(intent, mServiceConnection, BIND_ABOVE_CLIENT);
        }
        int left = mSharedPreferences.getInt(Constant.TAG_LEFT_VOLUME, 50);
        int right = mSharedPreferences.getInt(Constant.TAG_RIGHT_VOLUME, 50);
        setVolume(left, right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBinder != null) {
            unbindService(mServiceConnection);
        }
    }
}
