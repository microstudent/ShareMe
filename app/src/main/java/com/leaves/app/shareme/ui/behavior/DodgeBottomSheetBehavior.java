package com.leaves.app.shareme.ui.behavior;

import android.content.Context;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.lang.reflect.Field;

/**
 * Created by Leaves on 2017/3/29.
 */

public class DodgeBottomSheetBehavior extends BottomSheetBehavior<View> {
    private int mMinOffset;
    private boolean isScrollable;

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

    public void setScrollable(boolean scrollable) {
        isScrollable = scrollable;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, View child, MotionEvent event) {
        return isScrollable && super.onInterceptTouchEvent(parent, child, event);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, View child, MotionEvent event) {
        return isScrollable && super.onTouchEvent(parent, child, event);
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
