package com.leaves.app.shareme.widget.dialpad;

import android.support.v7.widget.RecyclerView;
import com.leaves.app.shareme.widget.dialpad.query.IQuery;


/**
 *
 * Created by MicroStudent on 2016/5/19.
 */
public interface INineKeyDialpad {
    /**
     * 显示九宫格面板
     */
    void show();

    /**
     * 隐藏九宫格面板
     */
    void hide();

    /**
     * 设置与之关联的RecyclerView，建立连接
     * @param recyclerView 建立连接的RecyclerView
     */
    void setRecyclerView(RecyclerView recyclerView);

    /**
     * 设置查询的查询器
     * @param query
     */
    void setQuery(IQuery query);
}
