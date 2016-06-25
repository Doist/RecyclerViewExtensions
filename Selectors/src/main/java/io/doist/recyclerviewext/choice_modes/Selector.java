package io.doist.recyclerviewext.choice_modes;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * {@link AbsListView}'s choice modes for {@link RecyclerView}.
 */
public abstract class Selector {
    public static final Object PAYLOAD_SELECT = new Object();

    private static final String KEY_SELECTOR_SELECTED_IDS = ":selector_selected_ids";

    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mAdapter;

    protected OnSelectionChangedListener mObserver;

    protected Selector(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        mRecyclerView = recyclerView;
        mAdapter = adapter;
        mAdapter.registerAdapterDataObserver(new SelectorAdapterDataObserver());
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

    public void setOnSelectionChangedListener(OnSelectionChangedListener observer) {
        mObserver = observer;
    }

    public void bind(RecyclerView.ViewHolder holder) {
        bind(holder, null);
    }

    /**
     * Binds the {@code holder} according to its selected state using {@link View#setActivated(boolean)}.
     *
     * When {@code payload} is not {@link #PAYLOAD_SELECT}, the background jumps immediately to the final state.
     * {@link #PAYLOAD_SELECT} is the payload used when the selection changes, where it's likely that the
     * animation (if any) should run.
     */
    public void bind(RecyclerView.ViewHolder holder, Object payload) {
        holder.itemView.setActivated(isSelected(holder.getItemId()));
        if (payload != PAYLOAD_SELECT) {
            // Ensure background jumps immediately to the current state instead of animating.
            Drawable background = holder.itemView.getBackground();
            if (background != null) {
                if (background instanceof LayerDrawable) {
                    LayerDrawable backgroundLayerDrawable = (LayerDrawable) background;
                    for (int i = 0; i < backgroundLayerDrawable.getNumberOfLayers(); ++i) {
                        backgroundLayerDrawable.getDrawable(i).jumpToCurrentState();
                    }
                }
                background.jumpToCurrentState();
            }
        }
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
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                mAdapter.notifyItemChanged(position, PAYLOAD_SELECT);
            }
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(Selector selector);
    }

    private class SelectorAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        private DeselectMissingIdsRunnable mDeselectMissingIdsRunnable = new DeselectMissingIdsRunnable();

        @Override
        public void onChanged() {
            mDeselectMissingIdsRunnable.run();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            // Deselect missing ids after all changes go through.
            mRecyclerView.removeCallbacks(mDeselectMissingIdsRunnable);
            mRecyclerView.post(mDeselectMissingIdsRunnable);
        }

        private class DeselectMissingIdsRunnable implements Runnable {
            @Override
            public void run() {
                int selectedCount = getSelectedCount();
                if (selectedCount > 0) {
                    // Build list of all selected ids that are still present.
                    List<Long> selectedIds = new ArrayList<>(selectedCount);
                    for (int i = 0; i < mAdapter.getItemCount(); i++) {
                        long id = mAdapter.getItemId(i);
                        if (isSelected(id)) {
                            selectedIds.add(id);
                        }
                    }

                    // Build set of missing ids.
                    HashSet<Long> missingIds = new HashSet<>(selectedCount);
                    for (long previouslySelectedId : getSelectedIds()) {
                        missingIds.add(previouslySelectedId);
                    }
                    missingIds.removeAll(selectedIds);

                    // Unselect all missing ids.
                    for (Long missingId : missingIds) {
                        setSelected(missingId, false);
                    }
                }
            }
        }
    }
}
