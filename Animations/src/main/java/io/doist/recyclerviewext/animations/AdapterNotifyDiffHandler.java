package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

/**
 * Diff handler that works directly with the adapter, calling the corresponding {@code notify*} method for each change.
 * It is expected that the associated data set changes have already been submitted.
 */
class AdapterNotifyDiffHandler implements DiffHandler {
    private RecyclerView.Adapter adapter;

    AdapterNotifyDiffHandler(RecyclerView.Adapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        adapter.notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        adapter.notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        adapter.notifyItemRangeRemoved(positionStart, itemCount);
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        adapter.notifyItemMoved(fromPosition, toPosition);
    }
}
