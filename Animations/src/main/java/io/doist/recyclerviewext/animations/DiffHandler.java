package io.doist.recyclerviewext.animations;

/**
 * Interface definition for a diff handler.
 */
interface DiffHandler {
    void onItemRangeChanged(int positionStart, int itemCount);

    void onItemRangeInserted(int positionStart, int itemCount);

    void onItemRangeRemoved(int positionStart, int itemCount);

    void onItemMoved(int fromPosition, int toPosition);
}
