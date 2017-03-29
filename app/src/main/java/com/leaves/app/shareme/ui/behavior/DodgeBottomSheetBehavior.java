package com.leaves.app.shareme.ui.behavior;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

import java.lang.reflect.Field;

/**
 * Created by Leaves on 2017/3/29.
 */

public class DodgeBottomSheetBehavior extends BottomSheetBehavior {
    private int mMinOffset;

    public DodgeBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        boolean result = super.onLayoutChild(parent, child, layoutDirection);
        reflectMinOffset(mMinOffset);
        return result;
    }

    public void setMinOffset(int minOffset) {
        mMinOffset = minOffset;
    }

    private void reflectMinOffset(int minOffset) {
        try {
            Field field = BottomSheetBehavior.class.getDeclaredField("mMinOffset");
            field.setAccessible(true);
            field.set(this, minOffset);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
