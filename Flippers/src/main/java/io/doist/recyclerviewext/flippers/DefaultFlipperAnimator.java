package io.doist.recyclerviewext.flippers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

class DefaultFlipperAnimator extends FlipperAnimator {
    private boolean mAnimating = false;

    @Override
    public void animateFlip(final View outView, final View inView) {
        if (mAnimating) {
            outView.animate().cancel();
            inView.animate().cancel();
        }

        outView.animate()
               .alpha(0f)
               .setDuration(getFlipDuration())
               .setListener(new AnimatorListenerAdapter() {
                   @Override
                   public void onAnimationStart(Animator animation) {
                       outView.setVisibility(View.VISIBLE);
                       outView.setAlpha(1f);
                   }

                   @Override
                   public void onAnimationEnd(Animator animation) {
                       outView.setVisibility(View.GONE);
                       outView.setAlpha(1f);
                   }
               })
               .withLayer();

        inView.animate()
              .alpha(1f)
              .setDuration(getFlipDuration())
              .setListener(new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationStart(Animator animation) {
                      inView.setVisibility(View.VISIBLE);
                      inView.setAlpha(0f);
                  }

                  @Override
                  public void onAnimationEnd(Animator animation) {
                      inView.setAlpha(1f);
                      mAnimating = false;
                  }
              })
              .withLayer();

        mAnimating = true;
    }

    @Override
    public boolean isAnimating() {
        return mAnimating;
    }
}
