package io.doist.recyclerviewext.animations;

import androidx.annotation.UiThread;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adds functionality to animate differences between an adapter's data set and a new one.
 *
 * @see AsyncDataSetDiffer
 */
public class DataSetDiffer {
    private final RecyclerView.Adapter adapter;
    private final Callback callback;

    private final Items items = new Items();
    private final ItemsObserver itemsObserver;

    private final AdapterNotifyDiffHandler adapterNotifyDiffHandler;

    /**
     * @param adapter  Adapter with which this data set differ is associated.
     * @param callback Callback that provides information about the items set in the adapter.
     */
    public DataSetDiffer(RecyclerView.Adapter adapter, Callback callback) {
        if (!adapter.hasStableIds()) {
            adapter.setHasStableIds(true);
        }
        this.adapter = adapter;
        this.callback = callback;
        this.itemsObserver = new ItemsObserver(items, callback);
        this.adapterNotifyDiffHandler = new AdapterNotifyDiffHandler(adapter);
        startObservingItems();
    }

    /**
     * Analyzes the data set using the supplied {@link Callback} and triggers all necessary {@code notify*} calls.
     */
    @UiThread
    public void diffDataSet() {
        // Pause adapter monitoring to avoid double counting changes.
        stopObservingItems();

        try {
            diffDataSet(adapterNotifyDiffHandler, callback);
        } finally {
            startObservingItems();
        }
    }

    void diffDataSet(final DiffHandler diffHandler, Callback callback) {
        // Snapshot IDs and content hashes before dispatching any adapter notifications.
        final int itemCount = callback.getItemCount();
        final Items adapterItems = new Items(itemCount);
        for (int i = 0; i < itemCount; i++) {
            adapterItems.add(callback.getItemId(i), callback.getItemContentHash(i));
        }

        // Detect the actual moved items instead of moving every row they pass.
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return adapterItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.getId(oldItemPosition) == adapterItems.getId(newItemPosition);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return items.getContentHash(oldItemPosition) == adapterItems.getContentHash(newItemPosition);
            }
        }, true);

        result.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                diffHandler.onItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                diffHandler.onItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                diffHandler.onItemMoved(fromPosition, toPosition);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                diffHandler.onItemRangeChanged(position, count);
            }
        });

        // Keep the existing Items instance: ItemsObserver tracks direct drag/collapse notifications in it.
        items.clear();
        items.ensureCapacity(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(adapterItems.getId(i), adapterItems.getContentHash(i));
        }
    }

    void startObservingItems() {
        adapter.registerAdapterDataObserver(itemsObserver);
    }

    void stopObservingItems() {
        adapter.unregisterAdapterDataObserver(itemsObserver);
    }

    /**
     * Callback for calculating the difference between the current data set and a new one.
     */
    public interface Callback {
        int getItemCount();

        /**
         * Return a unique id for this item, which is used to locate it in the data set.
         */
        long getItemId(int position);

        /**
         * Return a content hash of this item, which is used to detect changes in it.
         */
        long getItemContentHash(int position);
    }
}
