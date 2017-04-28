package com.leaves.app.shareme.presenter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.contract.AudioListContract;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Leaves on 2017/4/11.
 */

public class AudioListPresenter implements AudioListContract.Presenter {
    private final Context mContext;
    private AudioListContract.View mView;
    private ContentResolver mResolver;

    public AudioListPresenter(AudioListContract.View view, Context context) {
        mContext = context;
        mView = view;
    }

    @Override
    public void start() {
        mResolver = mContext.getContentResolver();
        Observable.just(mResolver)
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
                                Media media = new Media(image, src, title, duration, author, Media.AUDIO);
                                medias.add(media);
                            }
                            cursor.close();
                        }
                        Collections.sort(medias);
                        return medias;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Media>>() {
                    @Override
                    public void accept(List<Media> medias) throws Exception {
                        mView.setData(medias);
                    }
                });
    }

    private String getAlbumArt(long albumId) {
        Cursor cursor = mResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID+ "=?",
                new String[] {String.valueOf(albumId)},
                null);
        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            cursor.close();
            return path;
        }
        return null;
    }
}


