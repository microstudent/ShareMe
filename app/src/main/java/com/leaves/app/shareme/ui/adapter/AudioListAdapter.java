package com.leaves.app.shareme.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.SectionIndexer;

import com.bumptech.glide.Glide;
import com.leaves.app.shareme.GlideApp;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.ui.fragment.AudioListFragment;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Leaves on 2017/4/11.
 */

public class AudioListAdapter extends BaseAdapter<Media> implements BaseViewHolder.OnItemClickListener, SectionIndexer {
    private AudioListFragment.OnAudioClickListener mOnAudioClickListener;

    private List<Media> mSecetions;

    public AudioListAdapter() {
        super(R.layout.item_audio);
    }

    public void setOnAudioClickListener(AudioListFragment.OnAudioClickListener onAudioClickListener) {
        mOnAudioClickListener = onAudioClickListener;
    }

    @Override
    protected BaseViewHolder<Media> creatingHolder(View view, final Context context, int viewType) {
        BaseViewHolder<Media> viewHolder = new BaseViewHolder<Media>(view) {
            @Override
            public void setData(Media media) {
                setTextView(R.id.tv_title, media.getTitle());
                setTextView(R.id.tv_author, media.getArtist());
                ImageView imageView = (ImageView) findView(R.id.iv_cover);
                if (media.getImage() != null) {
                    GlideApp.with(context).load(media.getImage())
                            .centerCrop().error(R.drawable.ic_music).into(imageView);
                } else {
                    imageView.setImageDrawable(null);
                }
                setOnItemClickListener(AudioListAdapter.this);
            }
        };
        return viewHolder;
    }

    @Override
    public void OnItemClick(View view, int position) {
        if (mOnAudioClickListener != null) {
            mOnAudioClickListener.onAudioClick(mData.get(position));
        }
    }

    @Override
    public Object[] getSections() {
        if (mSecetions == null) {
            updateSelections();
        }
        return mSecetions.toArray();
    }

    /**
     * 前提是data有序
     */
    private void updateSelections() {
        mSecetions = new ArrayList<>();
        char lastChar = '#';
        for (Media media : mData) {
            String title = media.getTitle();
            if (!TextUtils.isEmpty(title)) {
                char c0 = title.charAt(0);
                if (Character.toLowerCase(lastChar) != Character.toLowerCase(c0)) {
                    mSecetions.add(media);
                }
                lastChar = c0;
            }
        }
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return mData.indexOf(mSecetions.get(sectionIndex));
    }

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }
}
