package com.leaves.app.shareme.widget.dialpad.ninekeybutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import com.leaves.app.shareme.R;


/**
 * Created by MicroStudent on 2016/5/19.
 */
public class NineKeyButton extends Button implements INineKeyButton {
    private SpannableStringBuilder mSpannableStringBuilder;
    private String mKeywords;
    private String mNumber;

    public NineKeyButton(Context context) {
        this(context, null);
    }

    public NineKeyButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NineKeyButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        handleAttrs(attrs);
        //最多两行
        setMaxLines(2);
        //一个格式化的文本Builder
        mSpannableStringBuilder = new SpannableStringBuilder("\n");
        //设置居中
        setGravity(Gravity.CENTER);
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
        mSpannableStringBuilder.clear();
        if (mKeywords == null && mNumber == null) {
            setVisibility(INVISIBLE);
        }
        if (mNumber != null) {
            mSpannableStringBuilder.append(mNumber);
            mSpannableStringBuilder.append("\n");
            mSpannableStringBuilder.setSpan(new RelativeSizeSpan(2.0f), 0, mNumber.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (mKeywords != null) {
            mSpannableStringBuilder.append(mKeywords);
        }
        setText(mSpannableStringBuilder);
    }

    @Override
    public void setKeyWord(String keyWord) {
        this.mKeywords = keyWord;
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

}
