package com.leaves.app.shareme.ui.widget.dialpad.ninekeybutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import com.leaves.app.shareme.R;
import com.leaves.app.shareme.util.DensityUtil;


/**
 * Created by MicroStudent on 2016/5/19.
 */
public class NineKeyButton extends View implements INineKeyButton {
    public static final int DEFAULT_NUMBER_TEXT_SIZE = 25;//sp
    public static final int DEFAULT_KEYWORD_TEXT_SIZE = 15;//sp
    public static final int DEFAULT_LINE_SPACE = 8;//dp行距

    private String mKeywords;
    private String mNumber;
    private TextPaint mTextPaint;
    private Rect mNumberRect, mKeywordRect;
    private int mNumberTextSize;
    private int mKeywordTextSize;
    private int mLineSpace;

    public NineKeyButton(Context context) {
        this(context, null);
    }

    public NineKeyButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NineKeyButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        handleAttrs(attrs);
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        //设置居中
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mNumberRect = new Rect();
        mKeywordRect = new Rect();
        mKeywordTextSize = DensityUtil.sp2px(context, DEFAULT_KEYWORD_TEXT_SIZE);
        mNumberTextSize = DensityUtil.sp2px(context, DEFAULT_NUMBER_TEXT_SIZE);
        mLineSpace = DensityUtil.dip2px(context, DEFAULT_LINE_SPACE);

        updateText();
    }

    private void handleAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.NineKeyButton);
            String keywords = array.getString(R.styleable.NineKeyButton_keywords);
            if (keywords != null) {
                setKeyWord(keywords);
            }
            String title = array.getString(R.styleable.NineKeyButton_number);
            if (title != null&&!title.isEmpty()) {
                setNumber(title.charAt(0));
            }
            array.recycle();
        }
    }


    private void updateText() {
        if (mKeywords == null && mNumber == null) {
            setVisibility(INVISIBLE);
            return;
        }
        if (mKeywords != null) {
            mTextPaint.setTextSize(mKeywordTextSize);
            mTextPaint.getTextBounds(mKeywords, 0, mKeywords.length(), mKeywordRect);
        }
        if (mNumber != null) {
            mTextPaint.setTextSize(mNumberTextSize);
            mTextPaint.getTextBounds(mNumber, 0, mNumber.length(), mNumberRect);
        }
        requestLayout();
    }

    @Override
    public void setKeyWord(String keyWord) {
        this.mKeywords = keyWord;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = 0, height = 0;

        int maxWidth = Math.max(mKeywordRect.width(), mNumberRect.width());
        int maxHeight = mKeywordRect.height() + mNumberRect.height();
        if (mNumber != null && mNumber.length() != 0) {
            maxHeight += mLineSpace;
        }
        if (mKeywords != null && mKeywords.length() != 0) {
            maxHeight += mLineSpace;
        }

        if (wMode == MeasureSpec.AT_MOST || wMode == MeasureSpec.UNSPECIFIED) {
            width = maxWidth + getPaddingRight() + getPaddingLeft();
        } else {
            width = wSize;
        }
        if (hMode == MeasureSpec.AT_MOST || hMode == MeasureSpec.UNSPECIFIED) {
            height = maxHeight + getPaddingBottom() + getPaddingTop();
        } else {
            height = hSize;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        mTextPaint.setTextSize(mNumberTextSize);
        Paint.FontMetricsInt fontMetricsInt = mTextPaint.getFontMetricsInt();
        canvas.drawText(mNumber, 0, mNumber.length(), width / 2, getPaddingTop() - fontMetricsInt.ascent, mTextPaint);
        mTextPaint.setTextSize(mKeywordTextSize);
        fontMetricsInt = mTextPaint.getFontMetricsInt();
        canvas.drawText(mKeywords, 0, mKeywords.length(), width / 2, mLineSpace + getPaddingTop() + mNumberRect.height() - fontMetricsInt.ascent, mTextPaint);
    }

    @Override
    public void setNumber(char title) {
        mNumber = String.valueOf(title);
    }

    @Override
    public String getKeyWord() {
        return mKeywords;
    }

    @Override
    public String getNumber() {
        return mNumber;
    }

    public void setTextColor(int color) {
        if (mTextPaint != null) {
            mTextPaint.setColor(color);
        }
    }
}
