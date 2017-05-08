package com.leaves.app.shareme.bean;

/**
 * 用于webSocket的事件
 * Created by Leaves on 2017/5/7.
 */

public class Message<T> {
    public static final int TYPE_PLAY = 0;
    public static final int TYPE_PAUSE = 1;
    public static final int TYPE_NEXT = 2;
    public static final int TYPE_PREV = 3;
    public static final int TYPE_LIST = 4;
    public static final int TYPE_MEDIA = 5;
    public static final int TYPE_RESUME = 6;
    public static final int TYPE_SYNC = 7;

    private int type;
    private T object;

    public Message(int type, T object) {
        this.type = type;
        this.object = object;
    }

    public int getType() {
        return type;
    }

    public void setType(int eventType) {
        this.type = eventType;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }
}
