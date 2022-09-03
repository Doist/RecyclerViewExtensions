package io.doist.recyclerviewext.choice_modes;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * {@link AbsListView}'s choice modes for {@link RecyclerView}.
 */
public abstract class Selector {
    public static final Object PAYLOAD_SELECT = new Object();

    private static final String KEY_SELECTOR_SELECTED_IDS = ":selector_selected_ids";

    protected final RecyclerView mRecyclerView;
    protected final RecyclerView.Adapter mAdapter;

    private List<OnSelectionChangedListener> mObservers = new ArrayList<>();

    // Used internally to disable item change notifications.
    // All selection changes lead to these notifications and it can be undesirable or inefficient.
    private boolean mNotifyItemChanges = true;

    protected Selector(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.Adapter adapter) {
        mRecyclerView = recyclerView;
        mAdapter = adapter;
        mAdapter.registerAdapterDataObserver(new SelectorAdapterDataObserver());
    }

    public abstract void setSelected(@NonNull String id, boolean selected);

    public void toggleSelected(@NonNull String id) {
        setSelected(id, !isSelected(id));
    }

    public abstract boolean isSelected(@NonNull String id);

    public abstract String[] getSelectedIds();

    public abstract int getSelectedCount();

    public abstract void clearSelected();

    public void addOnSelectionChangedListener(@NonNull OnSelectionChangedListener observer) {
        mObservers.add(observer);
    }

    public void removeOnSelectionChangedListener(@NonNull OnSelectionChangedListener observer) {
        mObservers.remove(observer);
    }

    public void removeAllOnSelectionChangedListeners() {
        mObservers.clear();
    }

    protected void onSelectionChanged(String[] selectedIds, String[] previousSelectedIds) {
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).onSelectionChanged(selectedIds, previousSelectedIds);
        }
    }

    /**
     * Binds the {@code holder} according to its selected state using
     * {@link View#setActivated(boolean)}.
     *
     * @param jumpToCurrentState When set, the background will have its {@link
     *                           Drawable#jumpToCurrentState()} called. In general, this should be
     *                           true for full binds, and false for partial binds that contain
     *                           {@link #PAYLOAD_SELECT}.
     */
    public boolean bind(@NonNull RecyclerView.ViewHolder holder, boolean jumpToCurrentState) {
        // TODO(Sergey): RecycleView IDs
        boolean isSelected = isSelected(Long.toString(holder.getItemId()));
        holder.itemView.setActivated(isSelected);

        if (jumpToCurrentState) {
            // Ensure background jumps immediately to the current state.
            Drawable background = holder.itemView.getBackground();
            if (background != null) {
                background.jumpToCurrentState();
            }
        }

        return isSelected;
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArray(KEY_SELECTOR_SELECTED_IDS, getSelectedIds());
    }

    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String[] selectedIds = savedInstanceState.getStringArray(KEY_SELECTOR_SELECTED_IDS);
            if (selectedIds != null) {
                mNotifyItemChanges = false;
                for (String selectedId : selectedIds) {
                    setSelected(selectedId, true);
                }
                mNotifyItemChanges = true;
            }
        }
    }

    protected void notifyItemChanged(@NonNull String id) {
        if (mNotifyItemChanges) {
            int position = RecyclerView.NO_POSITION;

            // TODO(Sergey): RecyclerView IDs
            // Look up the item position using findViewHolderForItemId().
            // This is fast and will work in most scenarios.
            RecyclerView.ViewHolder holder =
                    mRecyclerView.findViewHolderForItemId(Long.parseLong(id));
            if (holder != null) {
                position = holder.getAdapterPosition();
            }

            // RecyclerView can cache views offscreen that are not found by findViewHolderForItemId() et al,
            // but will be reattached without being rebound, so the adapter items must be iterated.
            // This is slower but prevents inconsistencies at the edges of the RecyclerView.
            if (position == RecyclerView.NO_POSITION) {
                for (int i = 0; i < mAdapter.getItemCount(); i++) {
                    // TODO(Sergey): RecyclerView IDs
                    if (Objects.equals(Long.toString(mAdapter.getItemId(i)), id)) {
                        position = i;
                        break;
                    }
                }
            }

            if (position != RecyclerView.NO_POSITION) {
                mAdapter.notifyItemChanged(position, PAYLOAD_SELECT);
            }
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(String[] selectedIds, String[] previousSelectedIds);
    }

    private class SelectorAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        private final DeselectMissingIdsRunnable mDeselectMissingIdsRunnable =
                new DeselectMissingIdsRunnable();

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
                    List<String> selectedIds = new ArrayList<>(selectedCount);
                    for (int i = 0; i < mAdapter.getItemCount(); i++) {
                        // TODO(Sergey): RecyclerView IDs
                        String id = Long.toString(mAdapter.getItemId(i));
                        if (isSelected(id)) {
                            selectedIds.add(id);
                        }
                    }

                    // Build set of missing ids.
                    HashSet<String> missingIds = new HashSet<>(selectedCount);
                    missingIds.addAll(Arrays.asList(getSelectedIds()));
                    missingIds.removeAll(selectedIds);

                    // Unselect all missing ids.
                    mNotifyItemChanges = false;
                    for (String missingId : missingIds) {
                        setSelected(missingId, false);
                    }
                    mNotifyItemChanges = true;
                }
            }
        }
    }
}
