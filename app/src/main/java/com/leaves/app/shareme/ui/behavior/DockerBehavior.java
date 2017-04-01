package com.leaves.app.shareme.ui.behavior;

import android.animation.AnimatorSet;
import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GravityCompat;
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

    private int mBottomSheetTop = 0;
    private AnimatorSet mAnimatorSet;
    private ObjectAnimator mChildAlphaAnimator;
    private ObjectAnimator mChildScaleXAnimator;
    private ObjectAnimator mChildScaleYAnimator;

    private ValueAnimator mExpandAnimator;
    private int mLastMeasureHeight;

    public DockerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return isViewHasBottomSheetBehavior(dependency);
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
            if (behavior.getState() == BottomSheetBehavior.STATE_DRAGGING || behavior.getState() == BottomSheetBehavior.STATE_SETTLING) {
                offsetChildAsNeeded(parent, child, dependency);
//            } else if (){
//                     measureChildAsNeeded(parent, child, dependency);
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
            if (mAnimatorSet == null) {
                initChildAnim(child);
            }
            if (mBottomSheetTop == 0) {
                mBottomSheetTop = dependency.getTop();
            }
            mChildAlphaAnimator.setCurrentPlayTime(mBottomSheetTop - dependency.getTop());
            mChildScaleXAnimator.setCurrentPlayTime(mBottomSheetTop - dependency.getTop());
            mChildScaleYAnimator.setCurrentPlayTime(mBottomSheetTop - dependency.getTop());

//            ViewCompat.offsetTopAndBottom(child, (dependency.getTop() - child.getBottom()));
        }
    }

    private void initChildAnim(View child) {
        mChildAlphaAnimator = ObjectAnimator.ofObject(child, "alpha", new FloatEvaluator(), 1, 0.7f);
        mChildScaleXAnimator = ObjectAnimator.ofObject(child, "scaleX", new FloatEvaluator(), 1, 0.9f);
        mChildScaleYAnimator = ObjectAnimator.ofObject(child, "scaleY", new FloatEvaluator(), 1, 0.9f);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.play(mChildAlphaAnimator).with(mChildScaleXAnimator).with(mChildScaleYAnimator);
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        boolean handled = false;
        int availableHeight = -1;
        final int childLpHeight = child.getLayoutParams().height;

        final List<View> dependencies = parent.getDependencies(child);
        final View bottomSheet = findFirstBottomSheet(dependencies);
        if (bottomSheet != null) {
            availableHeight = View.MeasureSpec.getSize(parentHeightMeasureSpec);
            if (availableHeight == 0) {
                // If the measure spec doesn't specify a size, use the current height
                availableHeight = parent.getHeight();
            }
            ViewGroup.LayoutParams params = child.getLayoutParams();
            int marginV = 0;
            if (params instanceof ViewGroup.MarginLayoutParams) {
                marginV = ((ViewGroup.MarginLayoutParams) params).topMargin + ((ViewGroup.MarginLayoutParams) params).bottomMargin;
            }
            availableHeight -= (bottomSheet.getMeasuredHeight() - getScrollRange(bottomSheet));
            int measureHeight = availableHeight - marginV;
            if (availableHeight >= 0/* && !handleByAnim(child, measureHeight)*/) {
                final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(availableHeight,
                        childLpHeight == ViewGroup.LayoutParams.MATCH_PARENT
                                ? View.MeasureSpec.EXACTLY
                                : View.MeasureSpec.AT_MOST);

//                    Now measure the scrolling view with the correct height
                parent.onMeasureChild(child, parentWidthMeasureSpec,
                        widthUsed, heightMeasureSpec, heightUsed);
            }
            mLastMeasureHeight = measureHeight;
            handled = true;
        }
        return handled;
    }

    private boolean handleByAnim(View child, int measureHeight) {
        if (mLastMeasureHeight > 0) {
            //do anim
            initExpandAnim(child, mLastMeasureHeight, measureHeight);
            mExpandAnimator.start();
            return true;
        }
        return false;
    }

    private void initExpandAnim(final View child, int from, int to) {
        if (mExpandAnimator != null) {
            mExpandAnimator.cancel();
            mExpandAnimator.removeAllUpdateListeners();
        }
        mExpandAnimator = ValueAnimator.ofInt(from, to);
        mExpandAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams params = child.getLayoutParams();
                if (params != null) {
                    params.height = (int) animation.getAnimatedValue();
                    child.setLayoutParams(params);
                }
            }
        });
    }

    private int getScrollRange(View bottomSheet) {
        if (mBottomSheetTop == 0) {
            mBottomSheetTop = bottomSheet.getTop();
        }
        BottomSheetBehavior behavior = getBottomSheetBehavior(bottomSheet);
        return bottomSheet.getMeasuredHeight() - behavior.getPeekHeight() + (bottomSheet.getTop() - mBottomSheetTop);
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
