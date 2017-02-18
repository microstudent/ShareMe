package com.leaves.app.shareme.widget.dialpad.listener;


import com.leaves.app.shareme.widget.dialpad.query.IQuery;

/**
 * Created by MicroStudent on 2016/5/19.
 */
public interface OnQueryTextListener {
    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     *
     */
    void onQueryTextChange(String newText);

    /**
     * 设置查询器
     * @param query 查询器
     */
    void setQuery(IQuery query);

    /**
     * 获得查询器
     * @return 查询器
     */
    IQuery getQuery();
}
