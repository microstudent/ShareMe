package com.leaves.app.shareme.widget.dialpad.ninekeybutton;

import android.view.View;

/**
 *
 * Created by MicroStudent on 2016/5/19.
 */
public interface INineKeyButton {
    void setKeyWord(String keyWord);

    void setNumber(char title);

    String getKeyWord();

    String getNumber();

    void setOnClickListener(View.OnClickListener l);
}
