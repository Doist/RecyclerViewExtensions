package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

/**
 * Adds functionality to animate differences between data sets automatically provided the ids are stable.
 */
public abstract class AnimatedAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private Items mItems = new Items();

    private boolean mAnimationsEnabled = true;
    private LocalStateObserver mLocalStateObserver = new LocalStateObserver();

    protected AnimatedAdapter() {
        setHasStableIds(true);
        registerAdapterDataObserver(mLocalStateObserver);
    }

    /**
     * Return the change hash for item at {@code position}. Used to notify adapter of changed items.
     *
     * Can be {@code 0} if the item never changes.
     */
    public abstract int getItemChangeHash(int position);

    /**
     * Returns whether animations are enabled or not.
     */
    public final boolean areAnimationsEnabled() {
        return mAnimationsEnabled;
    }

    /**
     * Sets whether animations are enabled or not (by default they are).
     *
     * If set to {@code false}, {@link #animateDataSetChanged()} proxies to {@link #notifyDataSetChanged()}.
     */
    public final void setAnimationsEnabled(boolean enabled) {
        if (mAnimationsEnabled != enabled) {
            mAnimationsEnabled = enabled;

            mItems.clear();

            if (mAnimationsEnabled) {
                registerAdapterDataObserver(mLocalStateObserver);
                mLocalStateObserver.onChanged(); // Rebuild animation ids and change hashes.
            } else {
                unregisterAdapterDataObserver(mLocalStateObserver);
            }
        }
    }

    /**
     * Analyzes the data set using {@link #getItemId(int)} and {@link #getItemChangeHash(int)} and makes the necessary
     * {@code notifyItem*} calls to go from the previous data  set to the new one.
     *
     * Note that ids are used to locate items in the list, while change hashes are used to detect changes in them.
     */
    public void animateDataSetChanged() {
        if (mAnimationsEnabled) {
            // Pause adapter monitoring to avoid double counting changes.
            unregisterAdapterDataObserver(mLocalStateObserver);

            // Prepare adapter items.
            int itemCount = getItemCount();
            Items adapterItems = new Items(itemCount);
            for (int i = 0; i < itemCount; i++) {
                adapterItems.add(getItemId(i), getItemChangeHash(i));
            }

            mItems.ensureCapacity(itemCount);

            // Remove all missing items up front to make positions more predictable in the second loop.
            int removePosition = -1;
            int removeCount = 0;
            for (int i = 0; i < mItems.size(); i++) {
                // Check if the item was removed.
                if (adapterItems.indexOfId(mItems.getId(i), i) == -1) {
                    mItems.remove(i);

                    if (removePosition == -1) {
                        removePosition = i;
                        removeCount = 1;
                    } else {
                        removeCount++;
                    }

                    i--;
                } else if (removePosition != -1) {
                    // Commit pending remove since the current is still there.
                    notifyItemRangeRemoved(removePosition, removeCount);
                    removePosition = -1;
                }
            }
            if (removePosition != -1) {
                notifyItemRangeRemoved(removePosition, removeCount);
            }

            // Add, change or move items based on their animation / change id.
            int insertPosition = -1;
            int insertCount = 0;
            int changePosition = -1;
            int changeCount = 0;
            for (int i = 0; i < itemCount; i++) {
                // Check if the item was inserted.
                int oldPosition = mItems.indexOfId(adapterItems.getId(i), i);
                if (oldPosition != -1) {
                    // Item was in the previous data set, it can have moved and / or changed.

                    // Commit pending insert since the current wasn't inserted and it'd conflict with the move / change.
                    if (insertPosition != -1) {
                        notifyItemRangeInserted(insertPosition, insertCount);
                        insertPosition = -1;
                    }

                    // Check if the item was moved.
                    if (oldPosition != i) {
                        // Commit pending change to avoid conflicts with the move added below.
                        if (changePosition != -1) {
                            notifyItemRangeChanged(changePosition, changeCount);
                            changePosition = -1;
                        }

                        long movedId = mItems.getId(oldPosition);
                        int movedChangeHash = mItems.getChangeHash(oldPosition);
                        mItems.remove(oldPosition);
                        mItems.add(i, movedId, movedChangeHash);
                    }
                    // Even if the item wasn't moved (stayed in the same position as in the previous selection) we must
                    // redraw it to make sure it's "compliant" with the new selection's view.
                    notifyItemChanged(oldPosition, i);

                    // Check if the item was changed.
                    if (mItems.getChangeHash(i) != adapterItems.getChangeHash(i)) {
                        mItems.setChangeHash(i, adapterItems.getChangeHash(i));

                        if (changePosition == -1) {
                            changePosition = i;
                            changeCount = 1;
                        } else {
                            changeCount++;
                        }
                    } else {
                        // Commit pending change since the current didn't change.
                        if (changePosition != -1) {
                            notifyItemRangeChanged(changePosition, changeCount);
                            changePosition = -1;
                        }
                    }
                } else {
                    // Item was not in the previous data set, it was added.

                    // Commit pending change now to avoid conflicts with the move added below.
                    if (changePosition != -1) {
                        notifyItemRangeChanged(changePosition, changeCount);
                        changePosition = -1;
                    }

                    mItems.add(i, adapterItems.getId(i), adapterItems.getChangeHash(i));

                    if (insertPosition == -1) {
                        insertPosition = i;
                        insertCount = 1;
                    } else {
                        insertCount++;
                    }
                }
            }
            if (changePosition != -1) {
                notifyItemRangeChanged(changePosition, changeCount);
            }
            if (insertPosition != -1) {
                notifyItemRangeInserted(insertPosition, insertCount);
            }

            // Resume adapter monitoring.
            registerAdapterDataObserver(mLocalStateObserver);
        } else {
            notifyDataSetChanged();
        }
    }

    /**
     * Keeps track of animation ids and change ids so that calculating operations between two lists (old and new) is
     * possible even if the user does not always use {@link #animateDataSetChanged()}.
     */
    private class LocalStateObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            int itemCount = getItemCount();
            mItems.clear();
            mItems.ensureCapacity(itemCount);
            for (int i = 0; i < itemCount; i++) {
                mItems.add(getItemId(i), getItemChangeHash(i));
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            for (int i = positionStart; i < positionStart + itemCount; i++) {
                mItems.setChangeHash(i, getItemChangeHash(i));
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mItems.ensureCapacity(mItems.size() + itemCount);
            for (int i = positionStart; i < positionStart + itemCount; i++) {
                mItems.add(i, getItemId(i), getItemChangeHash(i));
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mItems.remove(positionStart, positionStart + itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            boolean incrementPositions = fromPosition > toPosition;
            for (int i = 0; i < itemCount; i++) {
                long id = mItems.getId(fromPosition);
                int changeHash = mItems.getChangeHash(fromPosition);
                mItems.remove(fromPosition);
                mItems.add(toPosition, id, changeHash);
                if (incrementPositions) {
                    fromPosition++;
                    toPosition++;
                }
            }
        }
    }

    /**
     * Helper class to manage arrays of ids and change hashes.
     */
    private static class Items {
        private long[] mIds;
        private int[] mChangeHashes;
        private int mSize;

        public Items() {
            this(0);
        }

        public Items(int capacity) {
            mIds = new long[capacity];
            mChangeHashes = new int[capacity];
        }

        public long getId(int index) {
            return mIds[index];
        }

        public int getChangeHash(int index) {
            return mChangeHashes[index];
        }

        public int size() {
            return mSize;
        }

        public void setId(int index, long id) {
            mIds[index] = id;
        }

        public void setChangeHash(int index, int changeHash) {
            mChangeHashes[index] = changeHash;
        }

        public void add(long id, int changeHash) {
            if (mSize == mIds.length) {
                ensureCapacity(getNextSize());
            }
            mIds[mSize] = id;
            mChangeHashes[mSize] = changeHash;
            mSize++;
        }

        public void add(int index, long id, int changeHash) {
            if (mSize == mIds.length) {
                ensureCapacity(getNextSize());
            }
            System.arraycopy(mIds, index, mIds, index + 1, mSize - index);
            System.arraycopy(mChangeHashes, index, mChangeHashes, index + 1, mSize - index);
            mIds[index] = id;
            mChangeHashes[index] = changeHash;
            mSize++;
        }

        public void remove(int index) {
            remove(index, index + 1);
        }

        public void remove(int fromIndex, int toIndex) {
            System.arraycopy(mIds, toIndex, mIds, fromIndex, mSize - toIndex);
            System.arraycopy(mChangeHashes, toIndex, mChangeHashes, fromIndex, mSize - toIndex);
            mSize -= toIndex - fromIndex;
        }

        public void clear() {
            mSize = 0;
        }

        public void ensureCapacity(int minimumCapacity) {
            long[] ids = mIds;
            int[] changeHashes = mChangeHashes;
            if (ids.length < minimumCapacity) {
                mIds = new long[minimumCapacity];
                System.arraycopy(ids, 0, mIds, 0, mSize);
                mChangeHashes = new int[minimumCapacity];
                System.arraycopy(changeHashes, 0, mChangeHashes, 0, mSize);
            }
        }

        public int indexOfId(long id, int startPosition) {
            // Search back and forth until one of the ends is hit.
            for (int i = startPosition, j = 0; i >= 0 && i < mSize; j++, i += j % 2 == 0 ? j : -j) {
                if (id == mIds[i]) {
                    return i;
                }
            }
            if (startPosition < mSize / 2) {
                // Search forward if the head was hit.
                for (int i = Math.max(startPosition * 2 + 1, 0); i < mSize; i++) {
                    if (id == mIds[i]) {
                        return i;
                    }
                }
            } else if (startPosition > mSize / 2) {
                // Search backward if the tail was hit.
                for (int i = Math.min(mSize - (mSize - startPosition) * 2 - 1, mSize - 1); i >= 0; i--) {
                    if (id == mIds[i]) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private int getNextSize() {
            return mSize < 10 ? 10 : mSize + mSize / 2;
        }
    }
}
