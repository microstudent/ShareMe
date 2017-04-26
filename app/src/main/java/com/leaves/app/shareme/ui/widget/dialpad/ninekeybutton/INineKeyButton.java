package com.leaves.app.shareme.ui.widget.dialpad.ninekeybutton;

import android.view.View;

/**
 *
 * Created by MicroStudent on 2016/5/19.
 */
public interface INineKeyButton {
    void setKeyWord(String keyWord);

    void setNumber(String title);

    String getKeyWord();

    String getNumber();

    void setOnClickListener(View.OnClickListener l);
}
