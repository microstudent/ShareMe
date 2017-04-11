package com.leaves.app.shareme.eventbus;

/**
 * Created by Leaves on 2016/11/14.
 */

public class Message {
    private int mTag;
    private Object mObject;

    public Message(int tag, Object object) {
        mTag = tag;
        mObject = object;
    }

    public int getTag() {
        return mTag;
    }

    public Object getObject() {
        return mObject;
    }
}
