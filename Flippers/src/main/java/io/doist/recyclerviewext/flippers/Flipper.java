package io.doist.recyclerviewext.flippers;

import android.view.View;

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
        replaceInternal(outView, inView, true);
    }

    public void replaceNoAnimation(View outView, View inView) {
        replaceInternal(outView, inView, false);
    }

    protected void replaceInternal(View outView, View inView, boolean animate) {
        if (outView.getVisibility() != View.GONE || inView.getVisibility() != View.VISIBLE) {
            if (animate
                    && mFlipperAnimator != null
                    && outView.getWindowVisibility() == View.VISIBLE) {
                mFlipperAnimator.animateFlip(outView, inView);
            } else {
                outView.setVisibility(View.GONE);
                inView.setVisibility(View.VISIBLE);
            }
        }
    }
}
