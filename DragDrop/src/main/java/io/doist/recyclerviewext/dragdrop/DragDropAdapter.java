package io.doist.recyclerviewext.dragdrop;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Handles moving one item in another {@link RecyclerView.Adapter} without changing it by keeping track of its
 * original position, its current position and adjusting positions before proxying method the calls to the original
 * adapter. Besides position adjustments, it also monitors the original adapter for changes updating itself accordingly.
 *
 * Usage revolves around {@link RecyclerView#swapAdapter(RecyclerView.Adapter, boolean)}. Swap the original adapter with
 * this one, move the item around by calling {@link #setCurrentPosition(int)}, commit the move using
 * {@link #commitCurrentPosition()}, and finally swap this adapter with the original one.
 */
public class DragDropAdapter<VH extends RecyclerView.ViewHolder, T extends RecyclerView.Adapter<VH> & DragDrop>
        extends RecyclerView.Adapter<VH> {
    private DragDropManager mManager;
    private T mBaseAdapter;
    private int mDraggedPosition;
    private int mCurrentPosition;

    private Integer mStartBoundaryPosition = null;
    private Integer mEndBoundaryPosition = null;

    private BaseAdapterDataObserver mBaseAdapterDataObserver = new BaseAdapterDataObserver();

    public DragDropAdapter(DragDropManager manager, T baseAdapter, int position) {
        mManager = manager;
        mBaseAdapter = baseAdapter;
        mBaseAdapter.registerAdapterDataObserver(mBaseAdapterDataObserver);
        mDraggedPosition = mCurrentPosition = position;

        super.setHasStableIds(mBaseAdapter.hasStableIds());
    }

    /**
     * Returns the current position of the dragged item.
     */
    int getCurrentPosition() {
        return mCurrentPosition;
    }

    /**
     * Updates the current position of the dragged item.
     */
    void setCurrentPosition(int position) {
        if (mCurrentPosition != position) {
            int oldPosition = mCurrentPosition;
            mCurrentPosition = position;
            notifyItemMoved(oldPosition, mCurrentPosition);
            invalidateBoundaries();
        }
    }

    /**
     * Commits the current position to the original adapter.
     */
    void commitCurrentPosition() {
        mBaseAdapter.unregisterAdapterDataObserver(mBaseAdapterDataObserver);
        mBaseAdapter.moveItem(mDraggedPosition, mCurrentPosition);
        mDraggedPosition = mCurrentPosition;
        mBaseAdapter.registerAdapterDataObserver(mBaseAdapterDataObserver);
    }

    /**
     * Cleans up resources, namely the {@link RecyclerView.AdapterDataObserver} on the original adapter.
     */
    void destroy() {
        mDraggedPosition = mCurrentPosition = RecyclerView.NO_POSITION;
        mBaseAdapter.unregisterAdapterDataObserver(mBaseAdapterDataObserver);
        mBaseAdapter = null;
        mManager = null;
    }

    /**
     * Returns the start boundary for the dragged item. The result is cached until there are any adapter changes.
     */
    int getStartBoundaryPosition() {
        if (mStartBoundaryPosition == null) {
            if (mBaseAdapter instanceof DragDrop.Boundaries) {
                mStartBoundaryPosition =
                        ((DragDrop.Boundaries) mBaseAdapter).getDragStartBoundaryPosition(mDraggedPosition);
            } else {
                mStartBoundaryPosition = DragDrop.Boundaries.NO_BOUNDARY;
            }
        }
        return mStartBoundaryPosition;
    }

    /**
     * Returns the end boundary for the dragged item. The result is cached until there are any adapter changes.
     */
    int getEndBoundaryPosition() {
        if (mEndBoundaryPosition == null) {
            if (mBaseAdapter instanceof DragDrop.Boundaries) {
                mEndBoundaryPosition =
                        ((DragDrop.Boundaries) mBaseAdapter).getDragEndBoundaryPosition(mDraggedPosition);
            } else {
                mEndBoundaryPosition = DragDrop.Boundaries.NO_BOUNDARY;
            }
        }
        return mEndBoundaryPosition;
    }

    /**
     * Forces the recalculation of boundaries on the next request.
     */
    private void invalidateBoundaries() {
        mStartBoundaryPosition = null;
        mEndBoundaryPosition = null;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return mBaseAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(final VH holder, int position) {
        mBaseAdapter.onBindViewHolder(holder, getBasePosition(position));

        if (position == mCurrentPosition) {
            holder.setIsRecyclable(false);
            holder.itemView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mBaseAdapter.getItemViewType(getBasePosition(position));
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
        mBaseAdapter.setHasStableIds(hasStableIds);
    }

    @Override
    public long getItemId(int position) {
        return mBaseAdapter.getItemId(getBasePosition(position));
    }

    @Override
    public int getItemCount() {
        return mBaseAdapter.getItemCount();
    }

    public void onViewRecycled(VH holder) {
        mBaseAdapter.onViewRecycled(holder);
    }

    public boolean onFailedToRecycleView(VH holder) {
        if (holder.itemView.getVisibility() == View.INVISIBLE) {
            // Not recyclable and is invisible -- this must be the current item view holder. Fix state.
            holder.itemView.setVisibility(View.VISIBLE);
            holder.setIsRecyclable(true);
            return true;
        } else {
            return mBaseAdapter.onFailedToRecycleView(holder);
        }
    }

    public void onViewAttachedToWindow(VH holder) {
        mBaseAdapter.onViewAttachedToWindow(holder);
    }

    public void onViewDetachedFromWindow(VH holder) {
        mBaseAdapter.onViewDetachedFromWindow(holder);
    }

    // Don't override adapter data observer (un)registration, as it would block DragDropAdapter's.
    /*
    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        mBaseAdapter.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        mBaseAdapter.unregisterAdapterDataObserver(observer);
    }*/

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mBaseAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mBaseAdapter.onDetachedFromRecyclerView(recyclerView);
    }

    /**
     * Returns the appropriate position in the original adapter given a position in this adapter. Since the dragged item
     * moves around but this isn't reflected in the original adapter, it involves swapping the positions between the
     * original position of the dragged item and its current position in this adapter.
     */
    private int getBasePosition(int dragDropPosition) {
        if (dragDropPosition == mCurrentPosition) {
            return mDraggedPosition;
        } else if (mCurrentPosition < mDraggedPosition
                && dragDropPosition > mCurrentPosition && dragDropPosition <= mDraggedPosition) {
            return dragDropPosition - 1;
        } else if (mCurrentPosition > mDraggedPosition
                && dragDropPosition < mCurrentPosition && dragDropPosition >= mDraggedPosition) {
            return dragDropPosition + 1;
        } else {
            return dragDropPosition;
        }
    }

    /**
     * Reverse of {@link #getBasePosition(int)}.
     */
    private int getDragDropPosition(int basePosition) {
        if (basePosition == mDraggedPosition) {
            return mCurrentPosition;
        } else if (mCurrentPosition < mDraggedPosition
                && basePosition <= mCurrentPosition && basePosition > mDraggedPosition) {
            return basePosition - 1;
        } else if (mCurrentPosition > mDraggedPosition
                && basePosition >= mCurrentPosition && basePosition < mDraggedPosition) {
            return basePosition + 1;
        } else {
            return basePosition;
        }
    }

    /**
     * Monitors the original adapter for changes and triggers updates to this adapter accordingly.
     *
     * {@link #notifyItemRangeInserted(int, int)}, {@link #notifyItemRemoved(int)} and
     * {@link #notifyItemMoved(int, int)} all move the current dragged position its original position before applying
     * updates, moving again to the correct position afterwards. While a better algorithm could be used, it would still
     * be difficult to handle scrolling due to adapter changes. This approach is simple and works reliably.
     */
    private class BaseAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            throw new IllegalStateException(
                    String.format("%s#notifyDataSetChanged() cannot be called during drag",
                                  mBaseAdapter.getClass().getSimpleName()));
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            // Proxy change to this adapter, using the correct positions.
            for (int i = 0; i < itemCount; i++) {
                notifyItemChanged(getDragDropPosition(positionStart + i));
            }

            // Update item bitmap, as it changed.
            if (mDraggedPosition >= positionStart && mDraggedPosition < positionStart + itemCount) {
                mManager.postUpdateItemBitmap();
            }

            // Invalidate item boundaries, as it's unknown whether they have changed.
            invalidateBoundaries();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            // Move dragged item to its original position temporarily to make the inserted positions more predictable.
            if (mDraggedPosition != mCurrentPosition) {
                int oldCurrentPosition = mCurrentPosition;
                mCurrentPosition = mDraggedPosition;
                notifyItemMoved(oldCurrentPosition, mCurrentPosition);
            }

            // Update dragged / current position.
            if (mDraggedPosition > positionStart) {
                mDraggedPosition += itemCount;
                mCurrentPosition += itemCount;
            }

            // Proxy insert to this adapter.
            notifyItemRangeInserted(positionStart, itemCount);

            // Update item position after everything is measure and laid out in the appropriate locations.
            mManager.updateItemPosition();

            // Invalidate item boundaries, as it's unknown whether they have changed.
            invalidateBoundaries();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            // Cancel drag and drop if the dragged item is removed.
            if (mDraggedPosition >= positionStart && mDraggedPosition < positionStart + itemCount) {
                mManager.cancel();
                return;
            }

            // Move dragged item to its original position temporarily to make the removed positions more predictable.
            if (mDraggedPosition != mCurrentPosition) {
                int oldCurrentPosition = mCurrentPosition;
                mCurrentPosition = mDraggedPosition;
                notifyItemMoved(oldCurrentPosition, mCurrentPosition);
            }

            // Update dragged / current position.
            if (mDraggedPosition > positionStart) {
                mDraggedPosition -= itemCount;
                mCurrentPosition -= itemCount;
            }

            // Proxy remove to this adapter.
            notifyItemRangeRemoved(positionStart, itemCount);

            // Update item position after everything is measure and laid out in the appropriate locations.
            mManager.updateItemPosition();

            // Invalidate item boundaries, as it's unknown whether they have changed.
            invalidateBoundaries();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            // Move dragged item to its original position temporarily to make the moved positions more predictable.
            if (mDraggedPosition != mCurrentPosition) {
                int oldCurrentPosition = mCurrentPosition;
                mCurrentPosition = mDraggedPosition;
                notifyItemMoved(oldCurrentPosition, mCurrentPosition);
            }

            // Update dragged / current position.
            int offset = 0;
            if (mDraggedPosition >= fromPosition && mDraggedPosition < fromPosition + itemCount) {
                offset = toPosition - fromPosition;
            } else {
                if (fromPosition < toPosition) {
                    if (mDraggedPosition >= fromPosition + itemCount && mDraggedPosition < toPosition) {
                        offset = -itemCount;
                    }
                } else {
                    if (mDraggedPosition >= toPosition && mDraggedPosition < fromPosition) {
                        offset = itemCount;
                    }
                }
            }
            mDraggedPosition += offset;
            mCurrentPosition += offset;

            // Proxy move to this adapter.
            for (int i = 0; i < itemCount; i++) {
                notifyItemMoved(fromPosition, toPosition);
            }

            // Update item position after everything is measure and laid out in the appropriate locations.
            mManager.updateItemPosition();

            // Invalidate item boundaries, as it's unknown whether they have changed.
            invalidateBoundaries();
        }
    }
}
