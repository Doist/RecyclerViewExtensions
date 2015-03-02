package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds functionality to calculate differences between data sets automatically or manually.
 *
 * Calls to {@link #animateDataSetChanged()} will analyze the data set using {@link #getItemAnimationId(int)} and
 * {@link #getItemChangeId(int)} and calculate the necessary {@code notifyItem*} calls to go from the previous state to
 * the new state.
 *
 * If the data set is too big, this can have a significant performance impact. In those cases, it's best to use
 * {@link OpCalculator#get(List, List, List, List)} on a background thread, and call
 * {@link Op#notify(RecyclerView.Adapter)} for each returned {@link Op} right after switching to the new data set.
 *
 * Note that animation ids are used to locate items in the list, while change ids are used to detect changes in them.
 */
public abstract class AnimatedAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private ArrayList2<Object> mAnimationIds = new ArrayList2<>(0);
    private ArrayList2<Object> mChangeIds = new ArrayList2<>(0);

    /**
     * @param hasStableIds whether this {@link RecyclerView.Adapter} has stable ids. Given that a
     *                     {@link RecyclerView.AdapterDataObserver} is registered, it must be setup up-front.
     */
    protected AnimatedAdapter(boolean hasStableIds) {
        setHasStableIds(hasStableIds);
        registerAdapterDataObserver(new LocalStateObserver());
    }

    /**
     * Return the animation id for item at {@code position}. Used to animate items appearing, moving and disappearing.
     *
     * Must be non-null and unique across all objects in this adapter.
     */
    protected abstract Object getItemAnimationId(int position);

    /**
     * Return the change id for item at {@code position}. Can be {@code null}. Used to notify adapter of changed items.
     */
    protected abstract Object getItemChangeId(int position);

    /**
     * Analyzes the new data set using {@link #getItemAnimationId(int)} and {@link #getItemChangeId(int)} and makes the
     * necessary calls to the {@code notify*} methods.
     */
    public void animateDataSetChanged() {
        // TODO: Better architecture so that using {@link OpCalculator#getOps(List, List, List, List)} doesn't have a
        // performance penalty. The former needs 4 lists, clones 2 of them and builds an additional one. Here, we only
        // need 3 and don't clone or build any. For large data sets the difference is significant, and given this must
        // be used on the UI thread the logic is duplicated here.

        int itemCount = getItemCount();

        // Create new list of new animation ids.
        ArrayList<Object> toAnimationIds = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            toAnimationIds.add(getItemAnimationId(i));
        }

        // Remove up front to make positions more predictable in the second loop.
        for (int i = 0; i < mAnimationIds.size(); i++) {
            if (Utils.indexOf(toAnimationIds, mAnimationIds.get(i), i) == -1) {
                notifyItemRemoved(i);
                i--;
            }
        }

        // Ensure lists have the needed capacity ahead of time.
        mAnimationIds.ensureCapacity(itemCount);
        mChangeIds.ensureCapacity(itemCount);

        // Add, move or change items based on their animation / change id.
        for (int i = 0; i < itemCount; i++) {
            Object animationId = toAnimationIds.get(i);

            int oldPosition = Utils.indexOf(mAnimationIds, animationId, i);
            if (oldPosition != -1) {
                // Item was in the previous data set, it can have moved and / or changed.

                if (oldPosition != i) {
                    // Item was at a different location, it was moved.

                    notifyItemMoved(oldPosition, i);
                }

                if (!Utils.equals(mChangeIds.get(i), getItemChangeId(i))) {
                    // Item was changed.

                    notifyItemChanged(i);
                }
            } else {
                // Item was not in the previous data set, it was added.

                notifyItemInserted(i);
            }
        }
    }

    /**
     * Keeps track of animation ids and change ids so that calculating operations between two lists (old and new) is
     * possible even if the user does not always use {@link #animateDataSetChanged()}.
     */
    private class LocalStateObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            mAnimationIds.clear();
            mChangeIds.clear();

            int itemCount = getItemCount();
            for (int i = 0; i < itemCount; i++) {
                mAnimationIds.add(getItemAnimationId(i));
                mChangeIds.add(getItemChangeId(i));
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (itemCount > 1) {
                int positionEnd = positionStart + itemCount;
                for (int i = positionStart; i < positionEnd; i++) {
                    mChangeIds.set(i, getItemChangeId(i));
                }
            } else {
                mChangeIds.set(positionStart, getItemChangeId(positionStart));
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (itemCount > 1) {
                List<Object> newAnimationIds = new ArrayList<>(itemCount);
                List<Object> newChangeIds = new ArrayList<>(itemCount);
                int positionEnd = positionStart + itemCount;
                for (int i = positionStart; i < positionEnd; i++) {
                    newAnimationIds.add(getItemAnimationId(i));
                    newChangeIds.add(getItemChangeId(i));
                }
                mAnimationIds.addAll(positionStart, newAnimationIds);
                mChangeIds.addAll(positionStart, newChangeIds);
            } else {
                mAnimationIds.add(positionStart, getItemAnimationId(positionStart));
                mChangeIds.add(positionStart, getItemChangeId(positionStart));
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (itemCount > 1) {
                int positionEnd = positionStart + itemCount;
                mAnimationIds.removeRange(positionStart, positionEnd);
                mChangeIds.removeRange(positionStart, positionEnd);
            } else {
                mAnimationIds.remove(positionStart);
                mChangeIds.remove(positionStart);
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
                mChangeIds.addAll(toPosition, mChangeIds.subList(fromPosition, fromPositionEnd));
                mChangeIds.removeRange(removeStart, removeEnd);
            } else {
                mAnimationIds.add(toPosition, mAnimationIds.remove(fromPosition));
                mChangeIds.add(toPosition, mChangeIds.remove(fromPosition));
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
