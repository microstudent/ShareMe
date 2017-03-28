package com.leaves.app.shareme.ui.widget.dialpad;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.ui.widget.dialpad.animation.OnAnimationListener;
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnQueryTextListener;
import com.leaves.app.shareme.ui.widget.dialpad.ninekeybutton.NineKeyButton;
import com.leaves.app.shareme.util.DensityUtil;
import com.leaves.app.shareme.ui.widget.dialpad.listener.OnNumberClickListener;
import com.leaves.app.shareme.ui.widget.dialpad.ninekeybutton.INineKeyButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个九键拨号键盘
 * Created by MicroStudent on 2016/5/19.
 */
public class NineKeyDialpad extends FrameLayout implements INineKeyDialpad, View.OnClickListener {
    public static final int COLORFUL = -1;//键盘颜色

    private static final String TAG = "NineKeyDialpad";
    private List<NineKeyButton> mNineKeyButtons;

    private TableLayout mContainer;

    private OnQueryTextListener mOnQueryTextListener;

    private OnNumberClickListener mOnNumberClickListener;

    private int mTintColor;

    private ValueAnimator mDropAnimator;

    private OnAnimationListener mOnAnimationListener;

    private boolean isShown = true;//当前是否已经显示

    private StringBuilder mAppendingString;

    private int mDividerColor;

    private int mTintColor2;
    private ArgbEvaluator mArgbEvaluator;

    float[] mLines2Draw = new float[20];

    private Paint mDividerPaint;

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
        mArgbEvaluator = new ArgbEvaluator();

        initView();

        handleAttrs(attrs);

        loadAllNineKeyButton();
        for (int i = 0; i < mNineKeyButtons.size(); i++) {
            styleNineKeyButton(mNineKeyButtons.get(i), i);
        }

        setupListener();

        setWillNotDraw(false);
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
        mDividerPaint.setColor(mDividerColor);
        boolean isColorful = array.getBoolean(R.styleable.NineKeyDialpad_colorful, false);
        if (isColorful) {
            mTintColor2 = array.getColor(R.styleable.NineKeyDialpad_tint_color2, Color.BLACK);
        }
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
        mDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDividerPaint.setStrokeWidth(DensityUtil.dip2px(getContext(), 1));
    }

    private void setupListener() {
        for (INineKeyButton button : mNineKeyButtons) {
            button.setOnClickListener(this);
        }
    }

    private void loadAllNineKeyButton() {
        int count = mContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            TableRow row = (TableRow) mContainer.getChildAt(i);
            for (int j = 0; j < 3; j++) {
                View view = row.getChildAt(j);
                if (view instanceof NineKeyButton) {
                    mNineKeyButtons.add((NineKeyButton) view);
                }
            }
        }
    }

    private void styleNineKeyButton(NineKeyButton button, int index) {
        if (button != null && mNineKeyButtons != null && !mNineKeyButtons.isEmpty()) {
            float fun = (float) index / mNineKeyButtons.size();
            int color = (int) mArgbEvaluator.evaluate(fun, mTintColor, mTintColor2);
            button.setTextColor(color);
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
    public void onClick(View v) {
        if (v instanceof INineKeyButton) {
            handleNineKeyButtonOnClick((INineKeyButton) v);
            return;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //取0,3,6个button的bottom画横线
        if (mNineKeyButtons != null && !mNineKeyButtons.isEmpty() && mDividerPaint != null) {
            int lineCount = 0;
            for (int i = 0; i < 9; i += 3) {
                float y = ((ViewGroup) mNineKeyButtons.get(i).getParent()).getBottom();
                addDividerLine(lineCount, 0, y, getWidth(), y);
                lineCount++;
            }
            //画0，1个button的竖线
            float x1 = mNineKeyButtons.get(0).getRight();
            float x2 = mNineKeyButtons.get(1).getRight();
            addDividerLine(lineCount, x1, 0, x1, getHeight());
            lineCount++;
            addDividerLine(lineCount, x2, 0, x2, getHeight());
            canvas.drawLines(mLines2Draw, 0, mLines2Draw.length, mDividerPaint);
        }
    }

    private void addDividerLine(int lineIndex, float x1, float y1, float x2, float y2) {
        int index = lineIndex * 4;
        mLines2Draw[index] = x1;
        mLines2Draw[index + 1] = y1;
        mLines2Draw[index + 2] = x2;
        mLines2Draw[index + 3] = y2;
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
