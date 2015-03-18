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
    public StickyHeaderCanvasItemDecoration(T adapter) {
        super(adapter);
    }

    @Override
    protected void onDisplayStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, Canvas canvas,
                                         int x, int y) {
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
                manager.layoutDecorated(view, parent.getPaddingLeft(), 0,
                                        parent.getPaddingLeft() + view.getMeasuredWidth(), view.getMeasuredHeight());
            } else {
                manager.layoutDecorated(view, 0, parent.getPaddingTop(), view.getMeasuredWidth(),
                                        parent.getPaddingTop() + view.getMeasuredHeight());
            }
        }
    }

    @Override
    protected void onScrapStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent) {
        // Nothing to do. There's no container to remove the view from since it's draw manually.
    }
}
