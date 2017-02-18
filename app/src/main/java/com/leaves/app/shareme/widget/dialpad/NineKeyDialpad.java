package com.leaves.app.shareme.widget.dialpad;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.widget.dialpad.animation.OnAnimationListener;
import com.leaves.app.shareme.widget.dialpad.listener.OnNumberClickListener;
import com.leaves.app.shareme.widget.dialpad.listener.OnQueryTextListener;
import com.leaves.app.shareme.widget.dialpad.ninekeybutton.INineKeyButton;
import com.leaves.app.shareme.widget.dialpad.ninekeybutton.NineKeyButton;
import com.leaves.app.shareme.widget.dialpad.query.IQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个九键拨号键盘
 * Created by MicroStudent on 2016/5/19.
 */
public class NineKeyDialpad extends FrameLayout implements INineKeyDialpad, View.OnClickListener {

    private static final String TAG = "NineKeyDialpad";
    private List<INineKeyButton> mNineKeyButtons;

    private TableLayout mContainer;

    private OnQueryTextListener mOnQueryTextListener;

    private OnNumberClickListener mOnNumberClickListener;

    private RecyclerView mRecyclerView;

    private int mTintColor;

    private ValueAnimator mDropAnimator;

    private OnAnimationListener mOnAnimationListener;

    private boolean isShown = true;//当前是否已经显示

    private StringBuilder mAppendingString;

    private int mDividerColor;

    public NineKeyDialpad(Context context) {
        this(context, null);
    }

    public NineKeyDialpad(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NineKeyDialpad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.layout_ninekey_dialpad, this, true);
        mNineKeyButtons = new ArrayList<>();

        initView();

        handleAttrs(attrs);

        getAllNineKeyButton();

        setupListener();
    }

    /**
     * 初始化show和hide的动画
     */
    private void initAnimator() {
        mDropAnimator = ValueAnimator.ofInt(getHeight(), 0);
        mDropAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                getLayoutParams().height = (int) animation.getAnimatedValue();
                requestLayout();
            }
        });
        mDropAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mDropAnimator.setDuration(500);
        mDropAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mOnAnimationListener != null) {
                    mOnAnimationListener.onAnimationStart();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mOnAnimationListener != null) {
                    mOnAnimationListener.onAnimationFinish(isShown);
                }
            }
        });
    }

    private void handleAttrs(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.NineKeyDialpad);
        mTintColor = array.getColor(R.styleable.NineKeyDialpad_tint_color, Color.BLACK);
        mDividerColor = array.getColor(R.styleable.NineKeyDialpad_divider_color, Color.TRANSPARENT);
        array.recycle();
    }

    private Drawable tintColor(Drawable icon) {
        return tintColor(icon, mTintColor);
    }


    private Drawable tintColor(Drawable icon, int color) {
        Drawable result = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(result, color);
        return result;
    }

    private void initView() {
        mContainer = (TableLayout) findViewById(R.id.container);
    }

    private void setupListener() {
        for (INineKeyButton button : mNineKeyButtons) {
            button.setOnClickListener(this);
        }
    }

    private void getAllNineKeyButton() {
        int count = mContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            TableRow row = (TableRow) mContainer.getChildAt(i);
            for (int j = 0; j < 3; j++) {
                View view = row.getChildAt(j);
                if (view instanceof NineKeyButton) {
                    mNineKeyButtons.add((INineKeyButton) view);
                    styleNineKeyButton((Button) view);
                }
            }
        }
    }

    private void styleNineKeyButton(Button button) {
        if (button != null) {
            button.setTextColor(mTintColor);
        }
    }


    public void setOnQueryTextListener(OnQueryTextListener onQueryTextListener) {
        mOnQueryTextListener = onQueryTextListener;
    }

    public OnQueryTextListener getOnQueryTextListener() {
        return mOnQueryTextListener;
    }


    @Override
    public void show() {
        if (mDropAnimator == null) {
            initAnimator();
        }
        mDropAnimator.reverse();
        isShown = true;
    }

    @Override
    public void hide() {
        if (mDropAnimator == null) {
            initAnimator();
        }
        mDropAnimator.start();
        isShown = false;
    }

    @Override
    public void setRecyclerView(RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;

    }

    @Override
    public void setQuery(IQuery query) {
        if (mOnQueryTextListener != null) {
            mOnQueryTextListener.setQuery(query);
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof INineKeyButton) {
            handleNineKeyButtonOnClick((INineKeyButton) v);
            return;
        }
    }

    private void handleNineKeyButtonOnClick(INineKeyButton button) {
        if (mAppendingString == null) {
            mAppendingString = new StringBuilder(button.getNumber());
        } else {
            mAppendingString.append(button.getNumber());
        }
        if (mOnQueryTextListener != null) {
            mOnQueryTextListener.onQueryTextChange(mAppendingString.toString());
        }
        if (mOnNumberClickListener != null) {
            mOnNumberClickListener.onNumberClick(button.getNumber());
        }
    }

    public OnAnimationListener getOnAnimationListener() {
        return mOnAnimationListener;
    }

    public void setOnAnimationListener(OnAnimationListener mOnAnimationListener) {
        this.mOnAnimationListener = mOnAnimationListener;
    }

    public void setOnNumberClickListener(OnNumberClickListener onNumberClickListener) {
        mOnNumberClickListener = onNumberClickListener;
    }
}
