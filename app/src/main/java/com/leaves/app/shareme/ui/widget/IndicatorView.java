package com.leaves.app.shareme.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.util.DensityUtil;

/**
 * Created by zhangshuyue on 17-2-27.
 */
public class IndicatorView extends View{
    public static final int DEFAULT_DOT_COLOR = Color.WHITE;
    public static final int DEFAULT_DOT_WIDTH = 8;//dp
    public static final int DEFAULT_DOT_COUNT = 4;//长度
    public static final int DEFAULT_DOT_MARGIN = 6;//dp

    private int mDotWidth;
    private int mDotColor;
    private int mDotMargin;
    private int mDotCount;
    private boolean showClearIcon;

    private StringBuilder mStringBuilder;

    private Paint mPaint;

    public IndicatorView(Context context) {
        this(context, null);
    }

    public IndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        handleAttr(context, attrs);
    }

    private void handleAttr(Context context, AttributeSet attrs) {
        if (context != null && attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PasswordTextView);
            showClearIcon = array.getBoolean(R.styleable.PasswordTextView_show_clear_icon, false);
            mDotColor = array.getColor(R.styleable.PasswordTextView_dot_color, DEFAULT_DOT_COLOR);
            mDotWidth = array.getDimensionPixelOffset(R.styleable.PasswordTextView_dot_width, DensityUtil.dip2px(context, DEFAULT_DOT_WIDTH));
            mDotCount = array.getInt(R.styleable.PasswordTextView_password_length, DEFAULT_DOT_COUNT);
            array.recycle();
        }
        mDotMargin = DensityUtil.dip2px(context, DEFAULT_DOT_MARGIN);
        mPaint.setColor(mDotColor);
        mPaint.setStrokeWidth(mDotWidth);
    }

    private void initView() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStringBuilder = new StringBuilder();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = 0, height = 0;

        if (wMode == MeasureSpec.AT_MOST) {
            width = (2 * mDotMargin + mDotWidth) * mDotCount;
        } else if (wMode == MeasureSpec.EXACTLY) {
            width = wSize;
        }
        if (hMode == MeasureSpec.AT_MOST) {
            height = 2 * mDotMargin + mDotWidth;
        } else if (hMode == MeasureSpec.EXACTLY) {
            height = hSize;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(getWidth() / 2 - (2 * mDotMargin * mDotCount + mDotCount * mDotWidth) / 2, getHeight() / 2);
        int x = mDotMargin + mDotWidth / 2;
        int y = 0;
        for (int i = 0; i < mDotCount; i++) {
            canvas.drawCircle(x, y, mDotWidth / 2, mPaint);
            x += mDotWidth / 2 + 2 * mDotMargin;
        }
    }


    public String getPassword() {
        return mStringBuilder.toString();
    }

    public void append(String s) {
        mStringBuilder.append(s);
    }

}
