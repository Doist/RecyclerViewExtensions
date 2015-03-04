package io.doist.recyclerviewext.flippers;

import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewGroup;

/**
 * Flips between views with the same parent using a {@link SiblingAnimator}.
 */
public class SiblingFlipper {
    protected ViewGroup mContainer;
    private SiblingAnimator mSiblingAnimator = new DefaultSiblingAnimator();

    public SiblingFlipper(ViewGroup container) {
        mContainer = container;
    }

    public SiblingAnimator getSiblingAnimator() {
        return mSiblingAnimator;
    }

    public void setSiblingAnimator(SiblingAnimator siblingAnimator) {
        mSiblingAnimator = siblingAnimator;
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
                    && mSiblingAnimator != null
                    && outView.getWindowVisibility() == View.VISIBLE) {
                mSiblingAnimator.animateReplace(outView, inView);
            } else {
                outView.setVisibility(View.GONE);
                inView.setVisibility(View.VISIBLE);
            }
        }
    }
}
