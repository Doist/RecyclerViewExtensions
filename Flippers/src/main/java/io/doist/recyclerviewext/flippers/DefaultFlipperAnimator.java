package io.doist.recyclerviewext.flippers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

class DefaultFlipperAnimator extends FlipperAnimator {
    private AnimatorSet mAnimatorSet;

    @Override
    public void animateFlip(final View outView, final View inView) {
        // Ensure there's no animation running.
        endAnimation();

        // Both views must be visible for the animation to run.
        outView.setVisibility(View.VISIBLE);
        inView.setVisibility(View.VISIBLE);

        // Animate the replace.
        ObjectAnimator outAnimation = ObjectAnimator.ofFloat(outView, "alpha", 1f, 0f);
        ObjectAnimator inAnimation = ObjectAnimator.ofFloat(inView, "alpha", 0f, 1f);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setDuration(getFlipDuration()).playTogether(outAnimation, inAnimation);
        mAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimatorSet = null;

                outView.setVisibility(View.GONE);
            }
        });
        mAnimatorSet.start();
    }

    @Override
    public void endAnimation() {
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
        }
    }

    @Override
    public boolean isRunning() {
        return mAnimatorSet != null && mAnimatorSet.isRunning();
    }
}
