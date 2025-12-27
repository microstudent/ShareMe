package com.leaves.app.shareme.bean;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.microstudent.app.bouncyfastscroller.utils.CharacterUtils;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.io.Serializable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Leaves on 2016/11/13.
 */

public class Media extends RealmObject implements Serializable, Comparable<Media> {
    public static final int VIDEO = 0;
    public static final int AUDIO = 1;
    private String image;
    @PrimaryKey
    private String src;
    private String title;
    private long duration;
    private long albumId;
    private String artist;
    private int type;

    public Media() {
    }

    public Media(String image, String src, String title, long duration, String artist, int type) {
        this.image = image;
        this.src = src;
        this.title = title;
        this.duration = duration;
        this.artist = artist;
        this.type = type;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public int compareTo(@NonNull Media o) {
        String thisTitle = getTitle();
        String objTitle = o.getTitle();
        if (TextUtils.isEmpty(thisTitle) && TextUtils.isEmpty(objTitle)) {
            return 0;
        }
        if (TextUtils.isEmpty(thisTitle)) {
            return 1;
        }
        if (TextUtils.isEmpty(objTitle)) {
            return -1;
        }
        if (CharacterUtils.isChinese(thisTitle.charAt(0))) {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(thisTitle.charAt(0));
            if (pinyinArray != null) {
                thisTitle = pinyinArray[0];
            }
        }
        if (CharacterUtils.isChinese(objTitle.charAt(0))) {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(objTitle.charAt(0));
            if (pinyinArray != null) {
                objTitle = pinyinArray[0];
            }
        }
        return thisTitle.compareToIgnoreCase(objTitle);
    }

    @Override
    public String toString() {
        return title;
    }
}
