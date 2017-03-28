package com.leaves.app.shareme.ui.behavior;

import android.content.Context;
import android.graphics.Rect;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * dependency被观察者
 * child观察者
 * Created by Leaves on 2017/3/28.
 */

public class DockerBehavior extends CoordinatorLayout.Behavior<View> {

    final Rect mTempRect1 = new Rect();
    final Rect mTempRect2 = new Rect();


    public DockerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return isViewHasBottomSheetBehavior(dependency) || dependency instanceof AppBarLayout;
    }

    private boolean isViewHasBottomSheetBehavior(View dependency) {
        ViewGroup.LayoutParams params = dependency.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            return false;
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params)
                .getBehavior();
        if (!(behavior instanceof BottomSheetBehavior)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (isViewHasBottomSheetBehavior(dependency)) {
            BottomSheetBehavior behavior = getBottomSheetBehavior(dependency);
            if (behavior.getState() == BottomSheetBehavior.STATE_DRAGGING) {
                offsetChildAsNeeded(parent, child, dependency);
            } else if (behavior.getState() == BottomSheetBehavior.STATE_SETTLING) {
                measureChildAsNeeded(parent, child, dependency);
            }
        }
        return super.onDependentViewChanged(parent, child, dependency);

    }

    private void measureChildAsNeeded(CoordinatorLayout parent, View child, View dependency) {
        child.requestLayout();
    }

    private BottomSheetBehavior getBottomSheetBehavior(View dependency) {
        return BottomSheetBehavior.from(dependency);
    }

    private void offsetChildAsNeeded(CoordinatorLayout parent, View child, View dependency) {

            // Offset the child, pinning it to the bottom the header-dependency, maintaining
            // any vertical gap and overlap
        if (isViewHasBottomSheetBehavior(dependency)) {
            ViewCompat.offsetTopAndBottom(child, (dependency.getTop() - child.getBottom()));
        }
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        boolean handled = false;
        int height = 0;
        int availableHeight = -1;
        final int childLpHeight = child.getLayoutParams().height;
        if (childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT
                || childLpHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
            // If the menu's height is set to match_parent/wrap_content then measure it
            // with the maximum visible height

            final List<View> dependencies = parent.getDependencies(child);
            final View bottomSheet = findFirstBottomSheet(dependencies);
            if (bottomSheet != null) {
//                if (ViewCompat.getFitsSystemWindows(header)
//                        && !ViewCompat.getFitsSystemWindows(child)) {
//                    // If the header is fitting system windows then we need to also,
//                    // otherwise we'll get CoL's compatible measuring
//                    ViewCompat.setFitsSystemWindows(child, true);
//
//                    if (ViewCompat.getFitsSystemWindows(child)) {
//                        // If the set succeeded, trigger a new layout and return true
//                        child.requestLayout();
//                        return true;
//                    }
//                }

                availableHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec);
                if (availableHeight == 0) {
                    // If the measure spec doesn't specify a size, use the current height
                    availableHeight = parent.getHeight();
                }
                availableHeight -= bottomSheet.getMeasuredHeight();
            }

//            -------------------

//            final View header = findFirstHeader(dependencies);
//            if (header != null) {
//                if (ViewCompat.getFitsSystemWindows(header)
//                        && !ViewCompat.getFitsSystemWindows(child)) {
//                    // If the header is fitting system windows then we need to also,
//                    // otherwise we'll get CoL's compatible measuring
//                    ViewCompat.setFitsSystemWindows(child, true);
//
//                    if (ViewCompat.getFitsSystemWindows(child)) {
//                        // If the set succeeded, trigger a new layout and return true
//                        child.requestLayout();
//                        return true;
//                    }
//                }
//                if (availableHeight == -1) {
//                    availableHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec);
//                    if (availableHeight == 0) {
//                        // If the measure spec doesn't specify a size, use the current height
//                        availableHeight = parent.getHeight();
//                    }
//                }
//                availableHeight -= header.getMeasuredHeight();
//            }

            if (availableHeight >= 0) {
                final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(availableHeight,
                        childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT
                                ? View.MeasureSpec.EXACTLY
                                : View.MeasureSpec.AT_MOST);

                // Now measure the scrolling view with the correct height
                parent.onMeasureChild(child, parentWidthMeasureSpec,
                        widthUsed, heightMeasureSpec, heightUsed);
                handled = true;
            }
        }
        return handled;
    }
//
//    @Override
//    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
//        final List<View> dependencies = parent.getDependencies(child);
//        final View header = findFirstHeader(dependencies);
//
//        if (header != null) {
//            final CoordinatorLayout.LayoutParams lp =
//                    (CoordinatorLayout.LayoutParams) child.getLayoutParams();
//            final Rect available = mTempRect1;
//            available.set(parent.getPaddingLeft() + lp.leftMargin,
//                    header.getBottom() + lp.topMargin,
//                    parent.getWidth() - parent.getPaddingRight() - lp.rightMargin,
//                    parent.getHeight() + header.getBottom()
//                            - parent.getPaddingBottom() - lp.bottomMargin);
//
//            final Rect out = mTempRect2;
//            GravityCompat.apply(resolveGravity(lp.gravity), child.getMeasuredWidth(),
//                    child.getMeasuredHeight(), available, out, layoutDirection);
//
//            child.layout(out.left, out.top, out.right, out.bottom);
//        } else {
//            // If we don't have a dependency, let super handle it
//            super.onLayoutChild(parent, child, layoutDirection);
//        }
//        return true;
//    }

    private static int resolveGravity(int gravity) {
        return gravity == Gravity.NO_GRAVITY ? GravityCompat.START | Gravity.TOP : gravity;
    }

    private View findFirstHeader(List<View> views) {
        for (int i = 0, z = views.size(); i < z; i++) {
            View view = views.get(i);
            if (view instanceof AppBarLayout) {
                return (AppBarLayout) view;
            }
        }
        return null;
    }

    private View findFirstBottomSheet(List<View> dependencies) {
        for (View view : dependencies) {
            if (isViewHasBottomSheetBehavior(view)) {
                return view;
            }
        }
        return null;
    }

}
