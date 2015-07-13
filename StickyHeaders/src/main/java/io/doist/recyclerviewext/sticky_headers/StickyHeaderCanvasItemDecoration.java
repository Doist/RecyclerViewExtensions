package io.doist.recyclerviewext.sticky_headers;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Adds sticky headers capabilities to your {@link RecyclerView.Adapter} based on {@link Canvas} drawing.
 *
 * Fast, but limited. Since it's manually drawn, the sticky header has no support for animations and click listeners.
 * For a slightly slower but more powerful approach, see {@link StickyHeaderViewItemDecoration}.
 */
public class StickyHeaderCanvasItemDecoration<T extends RecyclerView.Adapter & StickyHeaders>
        extends StickyHeaderItemDecoration<T> {

    private boolean mDecorate;

    public StickyHeaderCanvasItemDecoration(T adapter) {
        this(adapter, true);
    }

    public StickyHeaderCanvasItemDecoration(T adapter, boolean vertical) {
        this(adapter, vertical, false);
    }

    public StickyHeaderCanvasItemDecoration(T adapter, boolean vertical, boolean reverse) {
        this(adapter, vertical, reverse, true);
    }

    /**
     * @param decorate true to layout header views with decoration insets; false, otherwise.
     */
    public StickyHeaderCanvasItemDecoration(T adapter, boolean vertical, boolean reverse, boolean decorate) {
        super(adapter, vertical, reverse);
        mDecorate = decorate;
    }

    @Override
    protected void onDisplayStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, Canvas canvas,
                                         float x, float y) {
        View view = stickyHeader.itemView;
        int count = canvas.save();
        canvas.translate(view.getLeft() + x, view.getTop() + y);
        view.draw(canvas);
        canvas.restoreToCount(count);
    }

    @Override
    protected void onCreateStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, boolean vertical,
                                        int position) {
        // Nothing to do. There's no container to add the view to since it's draw manually.
    }

    @Override
    protected void onBindStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, boolean vertical,
                                      int position) {
        RecyclerView.LayoutManager manager = parent.getLayoutManager();
        if (manager != null) {
            View view = stickyHeader.itemView;

            // Set the correct layout params.
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp == null) {
                view.setLayoutParams(manager.generateDefaultLayoutParams());
            } else if (!(lp instanceof RecyclerView.LayoutParams)) {
                view.setLayoutParams(manager.generateLayoutParams(lp));
            }

            // Measure and layout child.
            manager.measureChildWithMargins(view, 0, 0);
            if (vertical) {
                layout(manager, view, parent.getPaddingLeft(), 0, parent.getPaddingLeft() + view.getMeasuredWidth(),
                       view.getMeasuredHeight());
            } else {
                layout(manager, view, 0, parent.getPaddingTop(), view.getMeasuredWidth(),
                       parent.getPaddingTop() + view.getMeasuredHeight());
            }
        }
    }

    private void layout(RecyclerView.LayoutManager manager, View view, int left, int top, int right, int bottom) {
        if (mDecorate) {
            manager.layoutDecorated(view, left, top, right, bottom);
        } else {
            view.layout(left, top, right, bottom);
        }
    }

    @Override
    protected void onScrapStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent) {
        // Nothing to do. There's no container to remove the view from since it's draw manually.
    }
}
