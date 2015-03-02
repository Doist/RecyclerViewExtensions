package io.doist.recyclerviewext.flippers;

import android.view.View;

/**
 * Animates transitions between views with the same parent.
 */
public abstract class SiblingAnimator {
    private int mReplaceDuration = 250;

    public abstract void animateReplace(View out, View in);

    public abstract void endAnimation();

    public abstract boolean isRunning();

    public int getReplaceDuration() {
        return mReplaceDuration;
    }

    public void setReplaceDuration(int duration) {
        mReplaceDuration = duration;
    }
}
