package com.leaves.app.shareme.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.util.DensityUtil;

/**
 * Created by zhangshuyue on 17-2-27.
 */
public class PasswordTextView extends View{
    public static final int DEFAULT_DOT_COLOR = Color.WHITE;
    public static final int DEFAULT_DOT_WIDTH = 8;//dp
    public static final int DEFAULT_PASSWORD_LENGTH = 4;//长度
    public static final int DEFAULT_DOT_MARGIN = 6;//dp
    public static final int DEFAULT_TEXT_SIZE = 25;//sp

    private int mDotWidth;
    private int mDotColor;
    private int mDotMargin;
    private int mPasswordLength;
    private boolean showClearIcon;
    private int mTextSize;
    private String mPwStub = "_";

    private char[] mPasswords;
    private int mCurrentPwCount;

    private Paint mCirclePaint;
    private TextPaint mTextPaint;
    private boolean showPassword;

    public PasswordTextView(Context context) {
        this(context, null);
    }

    public PasswordTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordTextView(Context context, AttributeSet attrs, int defStyleAttr) {
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
            mTextSize = array.getDimensionPixelSize(R.styleable.PasswordTextView_text_size, DensityUtil.sp2px(context, DEFAULT_TEXT_SIZE));
            showPassword = array.getBoolean(R.styleable.PasswordTextView_show_password, true);
            mPasswordLength = array.getInt(R.styleable.PasswordTextView_password_length, DEFAULT_PASSWORD_LENGTH);
            mPasswords = new char[mPasswordLength];
            array.recycle();
        }
        mDotMargin = DensityUtil.dip2px(context, DEFAULT_DOT_MARGIN);
        mCirclePaint.setColor(mDotColor);
        mCirclePaint.setStrokeWidth(mDotWidth);
        mTextPaint.setColor(mDotColor);
        mTextPaint.setTextSize(mTextSize);
    }

    private void initView() {
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = 0, height = 0;

        if (wMode == MeasureSpec.AT_MOST) {
            width = (2 * mDotMargin + mDotWidth) * mPasswordLength;
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
        if (!showPassword) {
            //画圆
            canvas.translate(getWidth() / 2 - (2 * mDotMargin * mPasswordLength + mPasswordLength * mDotWidth) / 2, getHeight() / 2);
            int x = mDotMargin + mDotWidth / 2;
            int y = 0;
            for (int i = 0; i < mPasswordLength; i++) {
                canvas.drawCircle(x, y, mDotWidth / 2, mCirclePaint);
                x += mDotWidth / 2 + 2 * mDotMargin;
            }
        } else {
            //画密码
            canvas.translate(getWidth() / 2 - (2 * mDotMargin * mPasswordLength + mPasswordLength * mTextSize) / 2, getHeight() / 2);
            int x = mDotMargin + mTextSize / 2;
            int y = 0;
            for (int i = 0; i < mPasswordLength; i++) {
                if (mCurrentPwCount > i) {
                    canvas.drawText(String.valueOf(mPasswords[i]), x, y, mTextPaint);
                } else {
                    canvas.drawText(mPwStub, x, y, mTextPaint);
                }
                x += mTextSize / 2 + 2 * mDotMargin;
            }
        }
    }


    public String getPassword() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mCurrentPwCount; i++) {
            builder.append(mPasswords[i]);
        }
        return builder.toString();
    }

    public void append(String s) {
        if (s != null && mCurrentPwCount < mPasswordLength) {
            mPasswords[mCurrentPwCount] = s.charAt(0);
            mCurrentPwCount++;
            invalidate();
        }
    }

    public void clear() {
        mCurrentPwCount = 0;
        invalidate();
    }

    public void backspace() {
        if (mCurrentPwCount > 0) {
            mCurrentPwCount--;
        } else {
            mCurrentPwCount = 0;
        }
        invalidate();
    }
}
