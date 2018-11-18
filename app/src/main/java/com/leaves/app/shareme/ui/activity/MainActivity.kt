package com.leaves.app.shareme.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import butterknife.BindView
import butterknife.ButterKnife
import com.alibaba.android.arouter.facade.annotation.Route
import io.reactivex.functions.Consumer
import jp.wasabeef.blurry.Blurry

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.leaves.app.shareme.Constant
import com.leaves.app.shareme.GlideApp
import com.leaves.app.shareme.R
import com.leaves.app.shareme.RoutePath
import com.leaves.app.shareme.bean.Media
import com.leaves.app.shareme.contract.MainActivityContract
import com.leaves.app.shareme.presenter.MainPresenter
import com.leaves.app.shareme.ui.fragment.AudioListFragment
import com.leaves.app.shareme.ui.fragment.BehaviorFragment
import com.leaves.app.shareme.ui.fragment.BottomSheetFragment
import com.leaves.app.shareme.ui.fragment.ConnectionFragment
import com.leaves.app.shareme.ui.fragment.DialpadFragment
import com.leaves.app.shareme.ui.fragment.MusicFragment
import com.leaves.app.shareme.ui.fragment.PasswordFragment
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnNumberClickListener
import com.tbruyelle.rxpermissions2.RxPermissions

@Route(path = RoutePath.Main.it)
class MainActivity : AppCompatActivity(), OnNumberClickListener, BottomSheetFragment.OnFragmentMeasureListener, PasswordFragment.MainFragmentCallback, AudioListFragment.OnAudioClickListener, BehaviorFragment.OnBehaviorClickListener, MainActivityContract.View {
    private lateinit var mFragmentManager: FragmentManager

    @BindView(R.id.iv_bg)
    lateinit var mImageView: ImageView

    @BindView(R.id.activity_main)
    lateinit var mRootView: ViewGroup

    @BindView(R.id.view_content)
    lateinit var mContentView: View

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar

    private lateinit var mPasswordFragment: PasswordFragment
    private val mAudioListFragment: AudioListFragment by lazy {
        AudioListFragment.newInstance(mPresenter.role == Constant.ROLE_SERVER)
    }
    private lateinit var mPresenter: MainActivityContract.Presenter
    private var mMode = -1
    private lateinit var mMusicFragment: MusicFragment
    private val mConnectionFragment: ConnectionFragment by lazy {
        ConnectionFragment.newInstance(mPresenter.deviceList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        val rxPermissions = RxPermissions(this)

        rxPermissions.request(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe { aBoolean ->
                    if (!aBoolean) {
                        Toast.makeText(applicationContext, "必须赋予存储权限才可以使用", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        initView()
                        setupView()
                    }
                }
    }

    private fun initView() {
        mFragmentManager = supportFragmentManager
        mPresenter = MainPresenter(this, supportFragmentManager, this)
    }

    private fun setupView() {
        setSupportActionBar(mToolbar)
        title = ""
        //        mBottomSheetBehavior.setMinOffset(300);

        GlideApp.with(this)
                .asBitmap()
                .load(R.drawable.bg_piano)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        Blurry.with(this@MainActivity).radius(10).sampling(10).async().animate().from(resource).into(mImageView)
                        return false
                    }
                }).preload()
    }

    override fun setupFragment(mode: Int) {
        if (mMode != mode) {
            val fragment: Fragment
            when (mode) {
                MainPresenter.MODE_STARTUP -> {
                    mPasswordFragment = PasswordFragment.newInstance()
                    mFragmentManager.beginTransaction()
                            .replace(R.id.container_main, mPasswordFragment, PasswordFragment.TAG)
                            .commit()
                    fragment = DialpadFragment.newInstance()
                    mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, DialpadFragment.TAG).commit()
                    //                    switchFragment(BehaviorFragment.TAG, fragment, DialpadFragment.TAG, R.id.container_bottom, false);
                    mPresenter.cancelSearch()
                }
                MainPresenter.MODE_CONNECTED -> {
                    mMusicFragment = MusicFragment.newInstance(mPresenter.role == Constant.ROLE_SERVER)
                    mFragmentManager.beginTransaction()
                            .replace(R.id.container_main, mMusicFragment, MusicFragment.TAG)
                            .commit()
                    fragment = BehaviorFragment.newInstance()
                    mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, BehaviorFragment.TAG).commit()
                }
                else -> {
                    mPasswordFragment = PasswordFragment.newInstance()
                    mFragmentManager.beginTransaction().replace(R.id.container_main, mPasswordFragment, PasswordFragment.TAG).commit()
                    fragment = DialpadFragment.newInstance()
                    mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, DialpadFragment.TAG).commit()
                    mPresenter.cancelSearch()
                }
            }//                    switchFragment(DialpadFragment.TAG, fragment, BehaviorFragment.TAG, R.id.container_bottom, false);
        }
        mMode = mode
    }

    override fun onFragmentMeasure(width: Int, height: Int) {}

    override fun onDialpadClick(position: Int, number: String?) {
        mPasswordFragment.onDialpadClick(position, number)
        mPresenter.onDialpadClick(position, number)
    }

    /**
     * 由password通知的
     */
    override fun onSearchingDevice() {
        val fragment = BehaviorFragment.newInstance()
        mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, BehaviorFragment.TAG).commit()
    }

    override fun cancelSearch() {
        if (mPresenter != null) {
            mPresenter.cancelSearch()
        }
        val fragment = DialpadFragment.newInstance()
        mFragmentManager.beginTransaction().replace(R.id.layout_bottom, fragment, DialpadFragment.TAG).commit()
        //        switchFragment(BehaviorFragment.TAG, DialpadFragment.newInstance(), DialpadFragment.TAG, R.id.container_bottom, false);
    }

    private fun switchFragment(fromTag: String, to: Fragment, toTag: String, @IdRes resId: Int, addToBackStack: Boolean) {
        val transaction = mFragmentManager.beginTransaction()
        val fromFragment = mFragmentManager.findFragmentByTag(fromTag)
        if (fromFragment == null) {
            transaction.add(resId, to, toTag)
        } else if (to !== fromFragment) {
            if (!to.isAdded) {
                transaction.hide(fromFragment)
                        .add(resId, to, toTag)
                if (addToBackStack) {
                    transaction.addToBackStack(null)
                }
            } else {
                transaction.hide(fromFragment)
                        .show(to)
                if (addToBackStack) {
                    transaction.addToBackStack(null)
                }
            }
        }
        transaction.commit()
    }

    override fun onAudioClick(media: Media) {
        mMusicFragment.play(media)
        mAudioListFragment.dismiss()
    }

    override fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_exit -> {
                System.exit(0)
                return true
            }
            R.id.action_setting -> {
                val intent = Intent(this, SettingActivity::class.java)
                intent.putExtra(Constant.TAG_ROLE_TYPE, mPresenter.role)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSearching() {
        mPasswordFragment.switchToMode(PasswordFragment.MODE_SEARCHING)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        //        moveTaskToBack(true);
    }

    override fun onBehaviorClick(@IdRes id: Int, view: View) {
        if (id == R.id.bt_list) {
            if (mMusicFragment.isConnectionAlive) {
                switchFragment(AudioListFragment.TAG, mAudioListFragment, AudioListFragment.TAG, 0, false)
            } else {
                showToast("未能连接服务端/客户端")
            }
        } else if (id == R.id.bt_connection) {
            if (mMusicFragment.isConnectionAlive) {
                if (!mConnectionFragment.isAdded || mConnectionFragment.isDetached) {
                    switchFragment(MusicFragment.TAG, mConnectionFragment, ConnectionFragment.TAG, R.id.container_main, true)
                }
            } else {
                showToast("未能连接服务端/客户端")
            }
        }
    }

    companion object {

        val EXTRA_CURRENT_MEDIA_DESCRIPTION = "current_media"
    }
}
