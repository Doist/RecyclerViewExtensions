package io.doist.recyclerviewext.dragdrop;

import android.support.v7.widget.RecyclerView;

public interface DragDrop {
    int NO_BOUNDARY = -1;

    /**
     * Move item at {@code from} to {@code to}. The {@link RecyclerView.Adapter} must not be notified of any changes.
     */
    void moveItem(int from, int to);

    /**
     * Return the start boundary for the item at {@code position}. This is the top on a vertical (non-reverse) list.
     * If there is no boundary, return {@code NO_BOUNDARY}.
     */
    int getDragStartBoundaryPosition(int position);

    /**
     * Return the end boundary for the item at {@code position}. This is the bottom on a vertical (non-reverse) list.
     * If there is no boundary, return {@code NO_BOUNDARY}.
     */
    int getDragEndBoundaryPosition(int position);
}
