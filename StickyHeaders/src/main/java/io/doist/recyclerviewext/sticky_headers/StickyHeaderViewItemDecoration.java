package io.doist.recyclerviewext.sticky_headers;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * Adds sticky headers capabilities to your {@link RecyclerView.Adapter} by attaching a {@link View} to the
 * {@link RecyclerView}'s parent (which must be a {@link }
 *
 * Slightly slower than {@link StickyHeaderCanvasItemDecoration}, but more powerful. Your sticky headers can have click
 * listeners and run animations. However, the container of your {@link RecyclerView} must be a {@link FrameLayout},
 * {@link RelativeLayout}, or any other {@link ViewGroup} that allows children to be positioned absolutely using
 * margins.
 */
public class StickyHeaderViewItemDecoration<T extends RecyclerView.Adapter & StickyHeaders>
        extends StickyHeaderItemDecoration<T> {
    private WrapperViewGroup mWrapper;
    private float mOriginalTranslationX;
    private float mOriginalTranslationY;

    public StickyHeaderViewItemDecoration(Context context, T adapter) {
        this(context, adapter, true);
    }

    public StickyHeaderViewItemDecoration(Context context, T adapter, boolean vertical) {
        this(context, adapter, vertical, false);
    }

    public StickyHeaderViewItemDecoration(Context context, T adapter, boolean vertical, boolean reverse) {
        super(adapter, vertical, reverse);
        mWrapper = new WrapperViewGroup(context, vertical);
    }

    @Override
    protected void onDisplayStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, Canvas canvas,
                                         float x, float y) {
        mWrapper.setTranslationX(x);
        mWrapper.setTranslationY(y);
    }

    @Override
    protected void onCreateStickyHeader(RecyclerView.ViewHolder stickyHeader, final RecyclerView parent,
                                        final boolean vertical, int position) {
        // Keep the original translation values for restoring later.
        mOriginalTranslationX = stickyHeader.itemView.getTranslationX();
        mOriginalTranslationY = stickyHeader.itemView.getTranslationY();

        // Add the sticky header to the wrapper view.
        mWrapper.addViewInLayout(stickyHeader.itemView, -1, stickyHeader.itemView.getLayoutParams());

        // Add the wrapper to the parent's container. Do it on a post given this is called in RecyclerView#draw().
        parent.post(new Runnable() {
            @Override
            public void run() {
                // Add the wrapper view to the container.
                ViewGroup container = (ViewGroup) parent.getParent();
                container.addView(mWrapper,
                                  new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.WRAP_CONTENT,
                                                                   ViewGroup.MarginLayoutParams.WRAP_CONTENT));

                // Tweak its layout params, using the parent's margins / padding.
                ViewGroup.MarginLayoutParams parentParams = (ViewGroup.MarginLayoutParams) parent.getLayoutParams();
                ViewGroup.MarginLayoutParams wrapperParams = (ViewGroup.MarginLayoutParams) mWrapper.getLayoutParams();
                if (vertical) {
                    // Adjust width / height and horizontal margins for a vertical layout.
                    wrapperParams.leftMargin = parentParams.leftMargin + parent.getPaddingLeft();
                    wrapperParams.rightMargin = parentParams.rightMargin + parent.getPaddingRight();
                    wrapperParams.topMargin = parentParams.topMargin;
                    wrapperParams.bottomMargin = parentParams.bottomMargin;
                } else {
                    // Adjust width / height and vertical margins for a horizontal layout.
                    wrapperParams.leftMargin = parentParams.leftMargin;
                    wrapperParams.rightMargin = parentParams.rightMargin;
                    wrapperParams.topMargin = parentParams.topMargin + parent.getPaddingTop();
                    wrapperParams.bottomMargin = parentParams.bottomMargin + parent.getPaddingBottom();
                }
                mWrapper.setLayoutParams(wrapperParams);
            }
        });
    }

    @Override
    protected void onBindStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, boolean vertical,
                                      int position) {
        // Do nothing. Changes in the sticky header will trigger a re-measure / layout.
    }

    @Override
    protected void onScrapStickyHeader(final RecyclerView.ViewHolder stickyHeader, final RecyclerView parent) {
        // Restore original translation values.
        stickyHeader.itemView.setTranslationX(mOriginalTranslationX);
        stickyHeader.itemView.setTranslationY(mOriginalTranslationY);

        // Remove the sticky header from the wrapper.
        mWrapper.removeViewInLayout(stickyHeader.itemView);

        // Remove the wrapper from the parent's container. Do it on a post given this is called in RecyclerView#draw().
        parent.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup container = (ViewGroup) parent.getParent();
                container.removeView(mWrapper);
            }
        });
    }

    /**
     * Wrapper around the sticky header. Exists because the sticky header can be fetched from the
     * {@link RecyclerView.RecycledViewPool}, which is used to share views between {@link RecyclerView}s.
     * This means changing its layout params is dangerous and will lead to crashes, but by using a wrapper view
     * there's no need.
     */
    private static class WrapperViewGroup extends ViewGroup {
        private boolean mVertical;

        public WrapperViewGroup(Context context, boolean vertical) {
            super(context);
            mVertical = vertical;
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            View child = getChildAt(0);
            if (child != null) {
                child.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            View child = getChildAt(0);
            if (child != null) {
                LayoutParams lp = child.getLayoutParams();
                child.measure(RecyclerView.LayoutManager.getChildMeasureSpec(
                                      MeasureSpec.getSize(widthMeasureSpec), 0, lp.width, !mVertical),
                              RecyclerView.LayoutManager.getChildMeasureSpec(
                                      MeasureSpec.getSize(heightMeasureSpec), 0, lp.height, mVertical));
                setMeasuredDimension(child.getMeasuredWidth(), child.getMeasuredHeight());
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        @Override
        public boolean addViewInLayout(@NonNull View child, int index, LayoutParams params) {
            return super.addViewInLayout(child, index, params);
        }

        @Override
        public void removeViewInLayout(@NonNull View view) {
            super.removeViewInLayout(view);
        }

        @Override
        public boolean shouldDelayChildPressedState() {
            return false;
        }
    }
}
