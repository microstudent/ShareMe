package com.leaves.app.shareme.ui.adapter;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * RecyclerView的多页时的分页Adapter,包含空白View
 * Created by Leaves on 2016/9/2.
 */
public abstract class BaseAdapter<T> extends RecyclerView.Adapter<BaseViewHolder<T>>{
    public static final int TYPE_LOADING = 66;
    protected List<T> mData;
    protected Context mContext;
    protected int mItemLayoutRes = 0,mEmptyViewLayoutRes =0, mLoadingLayoutRes = 0;
    protected boolean showLoadingView = false;
    private boolean showEmptyView = true;

    public BaseAdapter(List<T> data, @LayoutRes int itemLayoutRes) {
        mItemLayoutRes = itemLayoutRes;
        mData = data;
    }

    public BaseAdapter(@LayoutRes int itemLayoutRes) {
        mItemLayoutRes = itemLayoutRes;
    }

    @Override
    public BaseViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        if ((mData == null || mData.isEmpty()) && showEmptyView && mEmptyViewLayoutRes != 0) {
            View view = LayoutInflater.from(mContext).inflate(mEmptyViewLayoutRes, parent, false);
            BaseViewHolder<T> viewHolder = new BaseViewHolder<T>(view) {
                @Override
                public void setData(T t) {
                }
            };
            viewHolder.setIsRecyclable(false);
            return viewHolder;
        }
        if (showLoadingView && viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(mContext).inflate(mLoadingLayoutRes, parent, false);
            return new BaseViewHolder<T>(view) {
                @Override
                public void setData(T t) {
                }
            };
        }
        View view = inflateView(parent, viewType);
        return creatingHolder(view, mContext, viewType);
    }

    private View inflateView(ViewGroup parent, int viewType) {
        return LayoutInflater.from(mContext).inflate(mItemLayoutRes, parent, false);
    }

    protected abstract BaseViewHolder<T> creatingHolder(View view, Context context, int viewType);


    @Override
    public int getItemViewType(int position) {
        if (showLoadingView && mData != null && position == mData.size()) {
            return TYPE_LOADING;
        }
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder<T> holder, int position) {
        if (mData != null && mData.size() != 0 && position < mData.size()) {
            T item = mData.get(position);
            holder.setData(item);
        }
    }

    @Override
    public int getItemCount() {
        if (mData == null || mData.size() == 0) {
            if (mEmptyViewLayoutRes != 0) {
                //显示空白View
                return 1;
            } else {
                return 0;
            }
        }
        if (showLoadingView) {
            //显示加载中的View
            return mData.size() + 1;
        }
        return mData.size();
    }

    public void setShowEmptyView(boolean showEmptyView) {
        this.showEmptyView = showEmptyView;
    }

    public void setData(List<T> data) {
        mData = data;
        notifyDataSetChanged();
    }


    public void addData(List<T> data) {
        if (mData == null) {
            mData = data;
            notifyDataSetChanged();
        } else {
            int from = mData.size();
            mData.addAll(data);
            notifyItemRangeInserted(from, data.size());
        }
    }

    public List<T> getData() {
        return mData;
    }

    public void setEmptyViewLayoutRes(int emptyViewLayoutRes) {
        mEmptyViewLayoutRes = emptyViewLayoutRes;
    }

    public void setLoadingLayoutRes(int loadingLayoutRes) {
        mLoadingLayoutRes = loadingLayoutRes;
    }

    public boolean isShowEmptyView() {
        return showEmptyView;
    }

    public void setShowLoadingView(boolean showLoadingView) {
        if (showLoadingView) {
            notifyItemInserted(getItemCount());
        } else {
            notifyItemRemoved(getItemCount());
        }
        this.showLoadingView = showLoadingView;
    }
}
