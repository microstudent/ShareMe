package com.leaves.app.shareme.wifidirect;

import android.app.Activity;
import android.app.Fragment;

/**
 * An interface for listener to {@link android.app.Fragment} and {@link android.app.Activity} lifecycle events.
 */
public interface LifecycleListener {

    /**
     * Callback for when {@link Fragment#onResume()}} or {@link Activity#onResume()} is called.
     */
    void onResume();

    /**
     * Callback for when {@link Fragment#onPause()}} or {@link Activity#onPause()}} is called.
     */
    void onPause();

    /**
     * Callback for when {@link android.app.Fragment#onDestroy()}} or {@link android.app.Activity#onDestroy()} is
     * called.
     */
    void onDestroy();
}
