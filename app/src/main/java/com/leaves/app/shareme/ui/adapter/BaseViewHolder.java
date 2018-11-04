package com.leaves.app.shareme.ui.adapter;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * 通用ViewHolder，大部分情况只需重写setData方法，通过setXXxX方法更新控件内容
 * Created by 45517 on 2016/3/21.
 */
public abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
    private static final int INVALID = -1;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public BaseViewHolder(View itemView) {
        super(itemView);

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public abstract void setData(T t);

    public void setTextView(@IdRes int id, String text) {
        TextView view = (TextView) findView(id);
        view.setText(text);
    }

    public void setTextView(@IdRes int id, @StringRes int text) {
        TextView view = (TextView) findView(id);
        view.setText(text);
    }

    public void setCheckBox(@IdRes int id, boolean b) {
        CheckBox cb = (CheckBox) findView(id);
        cb.setChecked(b);
    }

    public void setImageBitmap(@DrawableRes int id, @IdRes int viewid) {
        ImageView view = (ImageView) findView(viewid);
        if (view != null)
            view.setImageResource(id);
    }

    public void setVisibility(@IdRes int id, int visibility) {
        View view = findView(id);
        if (view != null) {
            view.setVisibility(visibility);
        }
    }


    public View findView(@IdRes int id) {
        return itemView.findViewById(id);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener m) {
        this.mOnItemLongClickListener = m;
    }

    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.OnItemClick(v, getLayoutPosition());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return mOnItemLongClickListener != null && mOnItemLongClickListener.OnItemLongClick(v, getLayoutPosition());
    }

    public interface OnItemClickListener {
        void OnItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean OnItemLongClick(View view, int position);
    }

    public void setOnClickListenerOn(View view, View.OnClickListener onClickListener) {
        if (view == null || view.getId() == INVALID) {
            return;
        }
        //adapterview does not support click listener
        if (view instanceof AdapterView) {
            return;
        }

        view.setOnClickListener(onClickListener);
    }
}
