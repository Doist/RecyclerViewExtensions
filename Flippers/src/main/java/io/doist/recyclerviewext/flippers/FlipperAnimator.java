package io.doist.recyclerviewext.flippers;

import android.view.View;

/**
 * Animates transitions between views with the same parent.
 */
public abstract class FlipperAnimator {
    private int mFlipDuration = 250;

    public abstract void animateFlip(View out, View in);

    public abstract void endAnimation();

    public abstract boolean isRunning();

    public int getFlipDuration() {
        return mFlipDuration;
    }

    public void setFlipDuration(int duration) {
        mFlipDuration = duration;
    }
}
