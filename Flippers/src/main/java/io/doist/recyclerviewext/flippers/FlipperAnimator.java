package io.doist.recyclerviewext.flippers;

import android.view.View;

/**
 * Animates transitions between views with the same parent.
 */
public abstract class FlipperAnimator {
    private long mFlipDuration = 250;

    public abstract void animateFlip(View out, View in);

    public long getFlipDuration() {
        return mFlipDuration;
    }

    public void setFlipDuration(long duration) {
        mFlipDuration = duration;
    }
}
