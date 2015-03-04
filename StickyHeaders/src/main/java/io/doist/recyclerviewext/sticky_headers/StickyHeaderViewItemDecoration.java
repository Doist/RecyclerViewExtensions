package io.doist.recyclerviewext.sticky_headers;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * Adds sticky headers capabilities to your {@link RecyclerView.Adapter} based on {@link Canvas} drawing. Your adapter
 * must implement {@link StickyHeaders} to indicate which items are headers.
 *
 * Slightly slower than {@link StickyHeaderCanvasItemDecoration}, but more powerful. Your sticky headers can have click
 * listeners and do animations. However, the container of your {@link RecyclerView} must be a {@link FrameLayout},
 * {@link RelativeLayout} or any other {@link ViewGroup} that allows children to be positioned absolutely using margins.
 */
public class StickyHeaderViewItemDecoration<T extends RecyclerView.Adapter & StickyHeaders>
        extends StickyHeaderItemDecoration<T> {
    private WrapperViewGroup mWrapper;
    private float mOriginalTranslationX;
    private float mOriginalTranslationY;

    public StickyHeaderViewItemDecoration(Context context, T adapter) {
        super(adapter);
        mWrapper = new WrapperViewGroup(context);
    }

    @Override
    protected void onDisplayStickyHeader(final RecyclerView.ViewHolder stickyHeader, final RecyclerView parent,
                                         final Canvas canvas, final int translationX, final int translationY) {
        stickyHeader.itemView.setTranslationX(translationX);
        stickyHeader.itemView.setTranslationY(translationY);
    }

    @Override
    protected void onCreateStickyHeader(final RecyclerView.ViewHolder stickyHeader, final RecyclerView parent,
                                        int position) {
        // Keep the original translation values for restoring later.
        mOriginalTranslationX = stickyHeader.itemView.getTranslationX();
        mOriginalTranslationY = stickyHeader.itemView.getTranslationY();

        // Add the sticky header to the wrapper view.
        mWrapper.addViewInLayout(stickyHeader.itemView, -1, stickyHeader.itemView.getLayoutParams());

        // Use the parent to measure and use the exact dimensions it'd have inside the list.
        RecyclerView.LayoutManager manager = parent.getLayoutManager();
        if (manager != null) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) stickyHeader.itemView.getLayoutParams();
            manager.measureChildWithMargins(stickyHeader.itemView, 0, 0);
            params.width = stickyHeader.itemView.getMeasuredWidth();
            params.height = stickyHeader.itemView.getMeasuredHeight();
            params.leftMargin = params.rightMargin = params.topMargin = params.bottomMargin = 0;
            stickyHeader.itemView.setLayoutParams(params);
        }

        // Add the wrapper to the parent's container. Do it on a post given this is called in RecyclerView#draw().
        parent.post(new Runnable() {
            @Override
            public void run() {
                boolean isVertical = isVertical(parent);

                // Add the view to the container.
                ViewGroup container = (ViewGroup) parent.getParent();
                container.addView(mWrapper);

                // Tweak its layout params, using the parent's margins / padding.
                ViewGroup.MarginLayoutParams parentParams = (ViewGroup.MarginLayoutParams) parent.getLayoutParams();
                ViewGroup.MarginLayoutParams wrapperParams = (ViewGroup.MarginLayoutParams) mWrapper.getLayoutParams();
                wrapperParams.width = wrapperParams.height = ViewGroup.MarginLayoutParams.WRAP_CONTENT;
                if (isVertical) {
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
    protected void onBindStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, int position) {
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
     * Wrapper around the sticky header. Exists for two reasons:
     * - The sticky header can be fetched from the {@link RecyclerView.RecycledViewPool}, which is used to share views
     * between {@link RecyclerView}s. This means changing its layout params is dangerous and will lead to crashes, but
     * by using a wrapper view we don't need to;
     * - Easy clipping of the sticky header when above the parent's padding / margin.
     */
    private static class WrapperViewGroup extends ViewGroup {
        public WrapperViewGroup(Context context) {
            super(context);
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
                child.measure(widthMeasureSpec, heightMeasureSpec);

                setMeasuredDimension(child.getMeasuredWidth(), child.getMeasuredHeight());
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        @Override
        public boolean addViewInLayout(View child, int index, LayoutParams params) {
            return super.addViewInLayout(child, index, params);
        }

        @Override
        public void removeViewInLayout(View view) {
            super.removeViewInLayout(view);
        }

        @Override
        public boolean shouldDelayChildPressedState() {
            return false;
        }
    }
}
