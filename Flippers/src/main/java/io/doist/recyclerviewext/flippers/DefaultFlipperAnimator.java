package io.doist.recyclerviewext.flippers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

class DefaultFlipperAnimator extends FlipperAnimator {
    @Override
    public void animateFlip(final View outView, final View inView) {
        final float prevOutAlpha = outView.getAlpha();
        outView.animate().alpha(0).setDuration(getFlipDuration()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                outView.setAlpha(prevOutAlpha);
                outView.setVisibility(View.GONE);
            }
        }).start();

        final float prevInAlpha = inView.getAlpha();
        inView.setVisibility(View.VISIBLE);
        inView.setAlpha(0);
        inView.animate().alpha(prevInAlpha).setDuration(getFlipDuration()).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                inView.setAlpha(prevInAlpha);
            }
        }).start();
    }
}
