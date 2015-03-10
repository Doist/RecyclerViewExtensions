package io.doist.recyclerviewext.flippers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

class DefaultFlipperAnimator extends FlipperAnimator {
    private boolean mAnimating = false;

    @Override
    public void animateFlip(final View outView, final View inView) {
        float initialOutAlpha;
        float initialInAlpha;
        if (mAnimating) {
            initialOutAlpha = outView.getAlpha();
            initialInAlpha = inView.getAlpha();
            outView.animate().cancel();
            inView.animate().cancel();
        } else {
            initialInAlpha = 0f;
            initialOutAlpha = 1f;
        }

        mAnimating = true;

        outView.setVisibility(View.VISIBLE);
        outView.setAlpha(initialOutAlpha);
        outView.animate().withLayer().alpha(0f).setDuration(getFlipDuration())
               .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                outView.setVisibility(View.GONE);
                outView.setAlpha(1f);
            }
        }).start();

        inView.setVisibility(View.VISIBLE);
        inView.setAlpha(initialInAlpha);
        inView.animate().withLayer().alpha(1f).setDuration(getFlipDuration())
              .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                inView.setAlpha(1f);
            }
        }).start();
    }

    @Override
    public boolean isAnimating() {
        return mAnimating;
    }
}
