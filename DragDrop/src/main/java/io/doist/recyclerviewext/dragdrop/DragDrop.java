package io.doist.recyclerviewext.dragdrop;

import android.support.v7.widget.RecyclerView;

public interface DragDrop {
    int NO_BOUNDARY = -1;

    /**
     * Move item at {@code from} to {@code to}. The {@link RecyclerView.Adapter} must be notified about the changes.
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

    /**
     * Adjusts any necessary properties of the {@code holder} that is being dragged.
     *
     * {@link #teardownDragViewHolder(RecyclerView.ViewHolder)} will be called sometime after this method and before
     * any other calls to this method go through.
     *
     * @return true if any changes require a re-layout, false if not.
     */
    boolean setupDragViewHolder(RecyclerView.ViewHolder holder);

    /**
     * Reverts any properties changed in {@link #setupDragViewHolder(RecyclerView.ViewHolder)}.
     *
     * Called after {@link #setupDragViewHolder(RecyclerView.ViewHolder)}.
     */
    void teardownDragViewHolder(RecyclerView.ViewHolder holder);
}
