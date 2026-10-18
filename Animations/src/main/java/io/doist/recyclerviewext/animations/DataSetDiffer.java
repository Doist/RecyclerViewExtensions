package io.doist.recyclerviewext.animations;

import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adds functionality to animate differences between an adapter's data set and a new one.
 *
 * @see AsyncDataSetDiffer
 */
public class DataSetDiffer {
    /**
     * The minimum distance a one-position shift must span to be notified as a single move of
     * the displaced item instead of one move per shifted item.
     * Shifts shorter than this keep the per-item move notifications.
     */
    private static final int MIN_ROTATION_LENGTH = 15;

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

        // Diff data set using the default diff handler and callback.
        diffDataSet(adapterNotifyDiffHandler, callback);

        // Resume adapter monitoring.
        startObservingItems();
    }

    void diffDataSet(DiffHandler diffHandler, Callback callback) {
        // Prepare adapter items.
        int itemCount = callback.getItemCount();
        Items adapterItems = new Items(itemCount);
        for (int i = 0; i < itemCount; i++) {
            adapterItems.add(callback.getItemId(i), callback.getItemContentHash(i));
        }

        items.ensureCapacity(itemCount);

        // Remove all missing items up front to make positions more predictable in the second loop.
        int removePosition = -1;
        int removeCount = 0;
        for (int i = 0; i < items.size(); i++) {
            // Check if the item was removed.
            if (adapterItems.indexOfId(items.getId(i), i) == -1) {
                items.remove(i);

                if (removePosition == -1) {
                    removePosition = i;
                    removeCount = 1;
                } else {
                    removeCount++;
                }

                i--;
            } else if (removePosition != -1) {
                // Commit pending remove since the current is still there.
                diffHandler.onItemRangeRemoved(removePosition, removeCount);
                removePosition = -1;
            }
        }
        if (removePosition != -1) {
            diffHandler.onItemRangeRemoved(removePosition, removeCount);
        }

        // Add, change or move items based on their animation / change id.
        int insertPosition = -1;
        int insertCount = 0;
        int changePosition = -1;
        int changeCount = 0;
        long blockedRotationId = RecyclerView.NO_ID;
        int rotationBlockedPosition = -1;
        for (int i = 0; i < itemCount; i++) {
            // Check if the item was inserted.
            int oldPosition = items.indexOfId(adapterItems.getId(i), i);
            if (oldPosition != -1) {
                // Item was in the previous data set, it can have moved and / or changed.

                // Commit pending insert since the current wasn't inserted and it'd conflict with the move / change.
                if (insertPosition != -1) {
                    diffHandler.onItemRangeInserted(insertPosition, insertCount);
                    insertPosition = -1;
                }

                // Check if the item was moved.
                if (oldPosition != i) {
                    // Commit pending change to avoid conflicts with the move added below.
                    if (changePosition != -1) {
                        diffHandler.onItemRangeChanged(changePosition, changeCount);
                        changePosition = -1;
                    }

                    // When a long run of items is shifted one position forward, and the displaced
                    // item lands right after the run - notify the displaced item's move once
                    // instead of notifying one move for each shifted item. Notifying each shifted
                    // item floods `RecyclerView` with operations when an item travels far.
                    // A failed probe remains valid until its first blocking position, so don't
                    // probe the same displaced item again while the fallback advances to it.
                    int rotationEnd = -1;
                    if (oldPosition == i + 1) {
                        long displacedId = items.getId(i);
                        if (displacedId != blockedRotationId ||
                                (rotationBlockedPosition != -1 && i >= rotationBlockedPosition)) {
                            RotationResult rotation = findRotation(adapterItems, i);
                            rotationEnd = rotation.end;
                            if (rotationEnd == -1) {
                                blockedRotationId = displacedId;
                                rotationBlockedPosition = rotation.blockedPosition;
                            } else {
                                blockedRotationId = RecyclerView.NO_ID;
                                rotationBlockedPosition = -1;
                            }
                        }
                    }
                    if (rotationEnd != -1) {
                        long displacedId = items.getId(i);
                        long displacedChangeHash = items.getContentHash(i);
                        items.remove(i);
                        items.add(rotationEnd, displacedId, displacedChangeHash);

                        diffHandler.onItemMoved(i, rotationEnd);
                    } else {
                        long movedId = items.getId(oldPosition);
                        long movedChangeHash = items.getContentHash(oldPosition);
                        items.remove(oldPosition);
                        items.add(i, movedId, movedChangeHash);

                        diffHandler.onItemMoved(oldPosition, i);
                    }
                }

                // Check if the item was changed.
                if (items.getContentHash(i) != adapterItems.getContentHash(i)) {
                    items.setContentHash(i, adapterItems.getContentHash(i));

                    if (changePosition == -1) {
                        changePosition = i;
                        changeCount = 1;
                    } else {
                        changeCount++;
                    }
                } else {
                    // Commit pending change since the current didn't change.
                    if (changePosition != -1) {
                        diffHandler.onItemRangeChanged(changePosition, changeCount);
                        changePosition = -1;
                    }
                }
            } else {
                // Item was not in the previous data set, it was added.

                // Commit pending change now to avoid conflicts with the move added below.
                if (changePosition != -1) {
                    diffHandler.onItemRangeChanged(changePosition, changeCount);
                    changePosition = -1;
                }

                items.add(i, adapterItems.getId(i), adapterItems.getContentHash(i));

                if (insertPosition == -1) {
                    insertPosition = i;
                    insertCount = 1;
                } else {
                    insertCount++;
                }
            }
        }
        if (changePosition != -1) {
            diffHandler.onItemRangeChanged(changePosition, changeCount);
        }
        if (insertPosition != -1) {
            diffHandler.onItemRangeInserted(insertPosition, insertCount);
        }
    }

    /**
     * Returns where the item currently at {@code start} lands if it displaced the following run,
     * or the first position that may unblock the rotation if it didn't.
     */
    RotationResult findRotation(Items newItems, int start) {
        long displacedId = items.getId(start);
        for (int i = start; i < newItems.size(); i++) {
            long newId = newItems.getId(i);
            if (newId == displacedId) {
                if (i - start < MIN_ROTATION_LENGTH || i >= items.size()) {
                    return RotationResult.blocked(i);
                }
                return new RotationResult(i, -1);
            }
            if (i + 1 >= items.size() || newId != items.getId(i + 1)) {
                return RotationResult.blocked(i);
            }
        }
        return RotationResult.none();
    }

    static class RotationResult {
        private static final RotationResult NONE = new RotationResult(-1, -1);

        private final int end;
        private final int blockedPosition;

        private RotationResult(int end, int blockedPosition) {
            this.end = end;
            this.blockedPosition = blockedPosition;
        }

        private static RotationResult none() {
            return NONE;
        }

        private static RotationResult blocked(int blockedPosition) {
            return new RotationResult(-1, blockedPosition);
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
