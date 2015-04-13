package io.doist.recyclerviewext.choice_modes;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link AbsListView}'s choice modes for {@link RecyclerView}.
 */
public abstract class Selector {
    private static final String KEY_SELECTOR_SELECTED_IDS = ":selector_selected_ids";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mAdapter;

    protected Selector(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        mRecyclerView = recyclerView;
        mAdapter = adapter;
        mAdapter.registerAdapterDataObserver(new SelectorObserver());
    }

    public abstract void setSelected(long id, boolean selected);

    public void toggleSelected(long id) {
        setSelected(id, !isSelected(id));
    }

    public abstract boolean isSelected(long id);

    public abstract long[] getSelectedIds();

    public abstract int getSelectedCount();

    public abstract void clearSelected();

    protected abstract void clearSelected(boolean notify);

    public void bind(RecyclerView.ViewHolder holder) {
        holder.itemView.setActivated(isSelected(holder.getItemId()));
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putLongArray(KEY_SELECTOR_SELECTED_IDS, getSelectedIds());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        long[] selectedIds = savedInstanceState.getLongArray(KEY_SELECTOR_SELECTED_IDS);
        if (selectedIds != null) {
            for (long selectedId : selectedIds) {
                setSelected(selectedId, true);
            }
        }
    }

    protected void notifyItemChangedIfVisible(long id) {
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForItemId(id);
        if (holder != null) {
            int position = holder.getLayoutPosition();
            if (position != RecyclerView.NO_POSITION) {
                mAdapter.notifyItemChanged(position);
            }
        }
    }

    private class SelectorObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            deselectMissingIds();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            deselectMissingIds();
        }

        private void deselectMissingIds() {
            int selectedCount = getSelectedCount();
            if (selectedCount > 0) {
                List<Long> selectedIds = new ArrayList<>(selectedCount);
                for (int i = 0; i < mAdapter.getItemCount(); i++) {
                    long id = mAdapter.getItemId(i);
                    if (isSelected(id)) {
                        selectedIds.add(id);
                    }
                }
                clearSelected(false);
                for (Long selectedId : selectedIds) {
                    setSelected(selectedId, true);
                }
            }
        }
    }
}
