package com.leaves.app.shareme.presenter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.contract.AudioListContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by Leaves on 2017/4/11.
 */

public class AudioListPresenter implements AudioListContract.Presenter, RealmChangeListener<RealmResults<Media>> {
    private static final String TAG = "AudioListPresenter";
    private final Context mContext;
    private AudioListContract.View mView;
    private ContentResolver mResolver;
    private Disposable mDisposable;
    private Realm mRealm;

    public AudioListPresenter(AudioListContract.View view, Context context) {
        mContext = context;
        mView = view;
    }

    @Override
    public void start() {
        mRealm = Realm.getDefaultInstance();
        RealmResults<Media> realmResults = mRealm.where(Media.class).findAll();
        realmResults.addChangeListener(this);
        mView.setData(mRealm.copyFromRealm(realmResults));

        mResolver = mContext.getContentResolver();
        mDisposable = Observable.just(mResolver)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Function<ContentResolver, List<Media>>() {
                    @Override
                    public List<Media> apply(ContentResolver contentResolver) throws Exception {
                        String[] queryProjection = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE
                                , MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST};
                        Cursor cursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, queryProjection, null, null, null);
                        List<Media> medias = new ArrayList<>();
                        if (cursor != null) {
                            while (cursor.moveToNext()) {
                                long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                                String src = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                                long duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                                String author = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                                String image = getAlbumArt(albumId);
                                if (!TextUtils.isEmpty(src) && src.endsWith("mp3")) {
                                    Media media = new Media(image, src, title, duration, author, Media.AUDIO);
                                    medias.add(media);
                                }
                            }
                            cursor.close();
                        }
                        Collections.sort(medias);
                        return medias;
                    }
                })
                .subscribe(new Consumer<List<Media>>() {
                    @Override
                    public void accept(List<Media> medias) throws Exception {
                        Realm realm = Realm.getDefaultInstance();
                        realm.beginTransaction();
                        realm.delete(Media.class);
                        realm.copyToRealmOrUpdate(medias);
                        realm.commitTransaction();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "get audio list fail", throwable);
                    }
                });
    }

    private String getAlbumArt(long albumId) {
        Cursor cursor = mResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[]{String.valueOf(albumId)},
                null);
        String path = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            }
            cursor.close();
        }
        return path;
    }

    public void onDestroy() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
        if (mRealm != null) {
            mRealm.close();
        }
    }

    @Override
    public void onChange(RealmResults<Media> medias) {
        mView.setData(mRealm.copyFromRealm(medias));
    }
}


