package io.doist.recyclerviewext.flippers;

import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewGroup;

/**
 * Flips between views with a common ancestor using a {@link FlipperAnimator}.
 */
public class Flipper {
    protected ViewGroup mContainer;
    private FlipperAnimator mFlipperAnimator = new DefaultFlipperAnimator();

    public Flipper(ViewGroup container) {
        mContainer = container;
    }

    public FlipperAnimator getFlipperAnimator() {
        return mFlipperAnimator;
    }

    public void setFlipperAnimator(FlipperAnimator flipperAnimator) {
        mFlipperAnimator = flipperAnimator;
    }

    public void replace(@IdRes int outId, @IdRes int inId) {
        replaceInternal(outId, inId, true);
    }

    public void replaceNoAnimation(@IdRes int outId, @IdRes int inId) {
        replaceInternal(outId, inId, false);
    }

    protected void replaceInternal(@IdRes int outId, @IdRes int inId, boolean animate) {
        View inView = mContainer.findViewById(inId);
        View outView = mContainer.findViewById(outId);

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
