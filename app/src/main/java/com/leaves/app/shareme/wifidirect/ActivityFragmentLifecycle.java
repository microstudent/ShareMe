package com.leaves.app.shareme.wifidirect;

import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.util.Util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A {@link Lifecycle} implementation for tracking and notifying listeners of
 * {@link android.app.Fragment} and {@link android.app.Activity} lifecycle events.
 */
class ActivityFragmentLifecycle {
    private final Set<LifecycleListener> lifecycleListeners =
            Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
    private boolean isResume;
    private boolean isDestroyed;

    /**
     * Adds the given listener to the list of listeners to be notified on each lifecycle event.
     *
     * <p>
     *     The latest lifecycle event will be called on the given listener synchronously in this method. If the
     *     activity or fragment is stopped, {@link LifecycleListener#onPause()}} will be called, and same for onStart and
     *     onDestroy.
     * </p>
     *
     * <p>
     *     Note - {@link LifecycleListener}s that are added more than once will have their
     *     lifecycle methods called more than once. It is the caller's responsibility to avoid adding listeners
     *     multiple times.
     * </p>
     */
    public void addListener(LifecycleListener listener) {
        lifecycleListeners.add(listener);

//        if (isDestroyed) {
//            listener.onDestroy();
//        } else if (isResume) {
//            listener.onResume();
//        } else {
//            listener.onPause();
//        }
    }

    void onResume() {
        isResume = true;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onResume();
        }
    }

    void onPause() {
        isResume = false;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onPause();
        }
    }

    void onDestroy() {
        isDestroyed = true;
        for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
            lifecycleListener.onDestroy();
        }
    }
}
