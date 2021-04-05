package io.doist.recyclerviewext.flippers;

import android.view.View;

import androidx.core.view.ViewCompat;

/**
 * Flips between views with a common ancestor using a {@link FlipperAnimator}.
 */
public class Flipper {
    private FlipperAnimator mFlipperAnimator = new DefaultFlipperAnimator();

    public FlipperAnimator getFlipperAnimator() {
        return mFlipperAnimator;
    }

    public void setFlipperAnimator(FlipperAnimator flipperAnimator) {
        mFlipperAnimator = flipperAnimator;
    }

    public void replace(View outView, View inView) {
        replace(outView, inView, null);
    }

    public void replace(View outView, View inView, Runnable afterReplaceAction) {
        replaceInternal(outView, inView, true);
        if (afterReplaceAction != null) {
            afterReplaceAction.run();
        }
    }

    public void replaceNoAnimation(View outView, View inView) {
        replaceNoAnimation(outView, inView, null);
    }

    public void replaceNoAnimation(View outView, View inView,
                                   Runnable afterReplaceAction) {
        replaceInternal(outView, inView, false);
        if (afterReplaceAction != null) {
            afterReplaceAction.run();
        }
    }

    protected void replaceInternal(View outView, View inView, boolean animate) {
        if (animate && mFlipperAnimator != null && ViewCompat.isLaidOut(outView)) {
            mFlipperAnimator.animateFlip(outView, inView);
        } else {
            if (mFlipperAnimator != null && mFlipperAnimator.isAnimating()) {
                outView.animate().cancel();
                inView.animate().cancel();
            }
            outView.setVisibility(View.GONE);
            inView.setVisibility(View.VISIBLE);
        }
    }
}
