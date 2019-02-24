package io.doist.recyclerviewext.flippers;

import android.view.View;

/**
 * Animates transitions between views with the same parent.
 */
public abstract class FlipperAnimator {
    public static long DEFAULT_DURATION = 250;

    private long mDuration = DEFAULT_DURATION;

    public abstract void animateFlip(View out, View in);

    public abstract boolean isAnimating();

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }
}
