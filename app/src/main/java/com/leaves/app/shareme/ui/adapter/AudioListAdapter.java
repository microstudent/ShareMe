package com.leaves.app.shareme.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.bean.Media;
import com.leaves.app.shareme.ui.fragment.AudioListFragment;

import hugo.weaving.DebugLog;

/**
 * Created by Leaves on 2017/4/11.
 */

public class AudioListAdapter extends BaseAdapter<Media> implements BaseViewHolder.OnItemClickListener {
    private AudioListFragment.OnAudioClickListener mOnAudioClickListener;
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
            @DebugLog
            public void setData(Media media) {
                setTextView(R.id.tv_title, media.getTitle());
                setTextView(R.id.tv_author,media.getArtist());
                ImageView imageView = (ImageView) findView(R.id.iv_cover);
                if (media.getImage() != null) {
                    Glide.with(context).load(media.getImage())
                            .centerCrop().crossFade().into(imageView);
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
}
