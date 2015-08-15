package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds functionality to calculate differences between data sets automatically or manually.
 *
 * Calls to {@link #animateDataSetChanged()} will analyze the data set using {@link #getItemAnimationId(int)} and
 * {@link #getItemChangeHash(int)} and calculate the necessary {@code notifyItem*} calls to go from the previous state to
 * the new state.
 *
 * If the data set is too big, this can have a significant performance impact. In those cases, it's best to use
 * {@link #prepareDataSetChanges(List, List)} on a background thread, and call {@link #animatePendingDataSetChanges()}
 * on the UI thread right after switching to the new data set.
 *
 * Note that animation ids are used to locate items in the list, while change hashes are used to detect changes in them.
 */
public abstract class AnimatedAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private ArrayList2<Object> mAnimationIds = new ArrayList2<>(0);
    private ArrayList2<Integer> mChangeHashes = new ArrayList2<>(0);

    private List<Op> mPendingOps = new ArrayList<>();
    private int mPendingSize = 0;

    private boolean mAnimationsEnabled = true;
    private LocalStateObserver mLocalStateObserver = new LocalStateObserver();

    /**
     * @param hasStableIds whether this {@link RecyclerView.Adapter} has stable ids. Given that a
     *                     {@link RecyclerView.AdapterDataObserver} is registered, it must be setup up-front.
     */
    protected AnimatedAdapter(boolean hasStableIds) {
        setHasStableIds(hasStableIds);
        registerAdapterDataObserver(mLocalStateObserver);
    }

    /**
     * Return the animation id for item at {@code position}. Used to animate items appearing, moving and disappearing.
     *
     * Must be non-null and unique across all objects in this adapter.
     */
    public abstract Object getItemAnimationId(int position);

    /**
     * Return the change hash for item at {@code position}. Used to notify adapter of changed items.
     *
     * Can be {@code null} if the item never changes.
     */
    public abstract Integer getItemChangeHash(int position);

    /**
     * Returns wether animations are enabled or not.
     */
    public final boolean areAnimationsEnabled() {
        return mAnimationsEnabled;
    }

    /**
     * Sets whether animations are enabled or not (by default they are).
     *
     * If set to {@code false}, {@link #animateDataSetChanged()} and {@link #animatePendingDataSetChanges()} proxy to
     * {@link #notifyDataSetChanged()}, and {@link #prepareDataSetChanges(List, List)} is a no-op.
     */
    public final void setAnimationsEnabled(boolean enabled) {
        if (mAnimationsEnabled != enabled) {
            mAnimationsEnabled = enabled;

            mAnimationIds.clear();
            mChangeHashes.clear();

            if (mAnimationsEnabled) {
                registerAdapterDataObserver(mLocalStateObserver);
                mLocalStateObserver.onChanged(); // Rebuild animation ids and change hashes.
            } else {
                unregisterAdapterDataObserver(mLocalStateObserver);
            }
        }
    }

    /**
     * Prepare data set changes to later be applied using {@link #animatePendingDataSetChanges()}.
     * The adapter must not change between both calls.
     */
    public synchronized void prepareDataSetChanges(List<Object> animationIds, List<Integer> changeHashes) {
        if (mAnimationsEnabled) {
            mPendingOps.clear();

            List<Object> currentAnimationIds = new ArrayList<>(mAnimationIds);
            List<Integer> currentChangeHashes = new ArrayList<>(mChangeHashes);

            mPendingSize = animationIds.size();

            int removeCount = 0;
            int insertCount = 0;

            // Remove all missing items up front to make positions more predictable in the second loop.
            Op.Remove removeOp = null;
            for (int i = 0; i < currentAnimationIds.size(); i++) {
                Object currentAnimationId = currentAnimationIds.get(i);

                // Check if the item was removed.
                if (Utils.indexOf(animationIds, currentAnimationId, i) == -1) {
                    currentAnimationIds.remove(i);
                    currentChangeHashes.remove(i);

                    if (removeOp == null) {
                        removeOp = new Op.Remove(i, 1);
                    } else {
                        removeOp.itemCount++;
                    }
                    removeCount++;

                    i--;
                } else if (removeOp != null) {
                    // Commit pending remove since the current is still there.
                    mPendingOps.add(removeOp);
                    removeOp = null;
                }
            }
            if (removeOp != null) {
                mPendingOps.add(removeOp);
            }

            // Add, move or change items based on their animation / change id.
            Op.Change changeOp = null;
            Op.Insert insertOp = null;
            for (int i = 0; i < mPendingSize; i++) {
                Object animationId = animationIds.get(i);

                // Check if the item was inserted.
                int oldPosition = Utils.indexOf(currentAnimationIds, animationId, i);
                if (oldPosition != -1) {
                    // Item was in the previous data set, it can have moved and / or changed.

                    // Commit pending insert since the current wasn't inserted and it'd conflict with the move / change.
                    if (insertOp != null) {
                        mPendingOps.add(insertOp);
                        insertOp = null;
                    }

                    // Check if the item was moved.
                    if (oldPosition != i) {
                        // Commit pending change to avoid conflicts with the move added below.
                        if (changeOp != null) {
                            mPendingOps.add(changeOp);
                            changeOp = null;
                        }

                        currentAnimationIds.add(i, currentAnimationIds.remove(oldPosition));
                        currentChangeHashes.add(i, currentChangeHashes.remove(oldPosition));

                        mPendingOps.add(new Op.Move(oldPosition, i));
                    }

                    // Check if the item was changed.
                    if (!Utils.equals(currentChangeHashes.get(i), changeHashes.get(i))) {
                        currentChangeHashes.set(i, changeHashes.get(i));

                        if (changeOp == null) {
                            changeOp = new Op.Change(i, 1);
                        } else {
                            changeOp.itemCount++;
                        }
                    } else {
                        // Commit pending change since the current didn't change.
                        if (changeOp != null) {
                            mPendingOps.add(changeOp);
                            changeOp = null;
                        }
                    }
                } else {
                    // Item was not in the previous data set, it was added.

                    // Commit pending change now to avoid conflicts with the move added below.
                    if (changeOp != null) {
                        mPendingOps.add(changeOp);
                        changeOp = null;
                    }

                    currentAnimationIds.add(i, animationIds.get(i));
                    currentChangeHashes.add(i, changeHashes.get(i));

                    if (insertOp == null) {
                        insertOp = new Op.Insert(i, 1);
                    } else {
                        insertOp.itemCount++;
                    }
                    insertCount++;
                }
            }
            if (changeOp != null) {
                mPendingOps.add(changeOp);
            }
            if (insertOp != null) {
                mPendingOps.add(insertOp);
            }

            // Check for inconsistencies (ie. duplicate animation ids) and bail out if they exist.
            if (mAnimationIds.size() + insertCount - removeCount != animationIds.size()) {
                mPendingOps.clear();
                mPendingOps.add(new Op.Unknown());
            }
        }
    }

    /**
     * Applies the changes previously determined by {@link #prepareDataSetChanges(List, List)}.
     * The adapter must not have changed between both calls.
     */
    private synchronized void animatePendingDataSetChanges() {
        if (mAnimationsEnabled) {
            // Ensure lists have the needed capacity ahead of time.
            mAnimationIds.ensureCapacity(mPendingSize);
            mChangeHashes.ensureCapacity(mPendingSize);

            // Apply ordered operations in the adapter.
            for (Op op : mPendingOps) {
                op.notify(this);
            }
            mPendingOps.clear();
        } else {
            notifyDataSetChanged();
        }
    }

    /**
     * Animates the adapter changes based on the {@link #getItemAnimationId(int)} and {@link #getItemChangeHash(int)} of
     * each item.
     */
    public synchronized void animateDataSetChanged() {
        if (mAnimationsEnabled) {
            // Prepare list of animation and change ids.
            int itemCount = getItemCount();
            List<Object> animationIds = new ArrayList<>(itemCount);
            List<Integer> changeHashes = new ArrayList<>(itemCount);
            for (int i = 0; i < itemCount; i++) {
                animationIds.add(getItemAnimationId(i));
                changeHashes.add(getItemChangeHash(i));
            }

            // Prepare changes and animate them.
            prepareDataSetChanges(animationIds, changeHashes);
            animatePendingDataSetChanges();
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
            mAnimationIds.clear();
            mChangeHashes.clear();
            mAnimationIds.ensureCapacity(itemCount);
            mChangeHashes.ensureCapacity(itemCount);
            for (int i = 0; i < itemCount; i++) {
                mAnimationIds.add(getItemAnimationId(i));
                mChangeHashes.add(getItemChangeHash(i));
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (itemCount > 1) {
                int positionEnd = positionStart + itemCount;
                for (int i = positionStart; i < positionEnd; i++) {
                    mChangeHashes.set(i, getItemChangeHash(i));
                }
            } else {
                mChangeHashes.set(positionStart, getItemChangeHash(positionStart));
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (itemCount > 1) {
                List<Object> newAnimationIds = new ArrayList<>(itemCount);
                List<Integer> newChangeHashes = new ArrayList<>(itemCount);
                int positionEnd = positionStart + itemCount;
                for (int i = positionStart; i < positionEnd; i++) {
                    newAnimationIds.add(getItemAnimationId(i));
                    newChangeHashes.add(getItemChangeHash(i));
                }
                mAnimationIds.addAll(positionStart, newAnimationIds);
                mChangeHashes.addAll(positionStart, newChangeHashes);
            } else {
                mAnimationIds.add(positionStart, getItemAnimationId(positionStart));
                mChangeHashes.add(positionStart, getItemChangeHash(positionStart));
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (itemCount > 1) {
                int positionEnd = positionStart + itemCount;
                mAnimationIds.removeRange(positionStart, positionEnd);
                mChangeHashes.removeRange(positionStart, positionEnd);
            } else {
                mAnimationIds.remove(positionStart);
                mChangeHashes.remove(positionStart);
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (itemCount > 1) {
                int fromPositionEnd = fromPosition + itemCount;
                int removeStart = fromPosition < toPosition ? fromPosition : fromPosition + itemCount;
                int removeEnd = removeStart + itemCount;
                mAnimationIds.addAll(toPosition, mAnimationIds.subList(fromPosition, fromPositionEnd));
                mAnimationIds.removeRange(removeStart, removeEnd);
                mChangeHashes.addAll(toPosition, mChangeHashes.subList(fromPosition, fromPositionEnd));
                mChangeHashes.removeRange(removeStart, removeEnd);
            } else {
                mAnimationIds.add(toPosition, mAnimationIds.remove(fromPosition));
                mChangeHashes.add(toPosition, mChangeHashes.remove(fromPosition));
            }
        }
    }

    /**
     * Same as {@link ArrayList}, but with a public {@link ArrayList#removeRange(int, int)}.
     */
    private static class ArrayList2<E> extends ArrayList<E> {
        private ArrayList2(int capacity) {
            super(capacity);
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }
    }
}
