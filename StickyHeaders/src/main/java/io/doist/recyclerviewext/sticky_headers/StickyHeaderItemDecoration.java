package io.doist.recyclerviewext.sticky_headers;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds sticky headers capabilities to your {@link RecyclerView.Adapter} based on {@link Canvas} drawing. Your adapter
 * must implement {@link StickyHeaders} to indicate which items are headers.
 *
 * Handles most of the background workflow, but the display logic behind it can differ depending on the implementation:
 * 1. {@link StickyHeaderCanvasItemDecoration} is the fastest implementation. However, it doesn't support click listeners
 * or animations for the sticky headers. It draws the sticky header manually on the {@link Canvas}.
 * 2. {@link StickyHeaderViewItemDecoration} is slightly slower and requires the {@link RecyclerView} parent to be a
 * {@link FrameLayout}, {@link RelativeLayout}, or any other {@link ViewGroup} that allows children to be positioned
 * absolutely using margins. However, since a real view is being added to the hierarchy, it has full support for click
 * listeners and animations.
 */
abstract class StickyHeaderItemDecoration<T extends RecyclerView.Adapter & StickyHeaders>
        extends RecyclerView.ItemDecoration {
    private T mAdapter;

    // Static layout properties. If set, querying the RecyclerView's LayoutManager can be avoided.
    private Boolean mIsVertical;
    private Boolean mIsReverse;

    // Header positions for the currently displayed list.
    private List<Integer> mHeaderPositions = new ArrayList<>(0);

    // Sticky header's ViewHolder and dirty state.
    private RecyclerView.ViewHolder mStickyHeader;
    private boolean mDirty;

    public StickyHeaderItemDecoration(T adapter) {
        mAdapter = adapter;
        adapter.registerAdapterDataObserver(new SectionInfoAdapterDataObserver());
    }

    /**
     * Sets the orientation and direction of the {@link RecyclerView.LayoutManager} used by this {@link RecyclerView}.
     * {@link LinearLayoutManager} is supported out of the box, but calling this ensures a slightly better performance.
     */
    public void setOrientationAndDirection(boolean isVertical, boolean isReverse) {
        mIsVertical = isVertical;
        mIsReverse = isReverse;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        int headerCount = mHeaderPositions.size();
        int childCount = parent.getChildCount();
        if (headerCount > 0 && childCount > 0) {
            // Find first valid child.
            View firstChild = null;
            int firstChildIndex = -1;
            int firstChildPos = -1;
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                if (!params.isItemRemoved() && !params.isViewInvalid()) {
                    firstChild = child;
                    firstChildIndex = i;
                    firstChildPos = params.getViewPosition();
                    break;
                }
            }

            if (firstChild != null) {
                int index = findOrderedIndex(mHeaderPositions, firstChildPos);
                int headerPos = index != -1 ? mHeaderPositions.get(index) : -1;
                // Show header if there's one to show, and if it's not the first view or is on the edge.

                if (headerPos != -1 && (headerPos != firstChildPos || isViewOnBoundary(parent, firstChild))) {
                    // Ensure existing sticky header, if any, is of correct type.
                    if (mStickyHeader != null
                            && mStickyHeader.getItemViewType() != mAdapter.getItemViewType(headerPos)) {
                        // A sticky header was setup before but is not of the correct type. Scrap it.º
                        scrapStickyHeader(parent);
                    }

                    // Ensure sticky header is created, if absent, or bound, if dirty or the position changed.
                    if (mStickyHeader == null) {
                        createStickyHeader(parent, headerPos);
                    }
                    if (mDirty || mStickyHeader.getPosition() != headerPos) {
                        bindStickyHeader(parent, headerPos);
                    }

                    // Draw the sticky header using translation values which depend on orientation, direction and the
                    // position of the next header view.
                    View nextHeaderView = null;
                    if (mHeaderPositions.size() > index + 1) {
                        int nextHeaderPos = mHeaderPositions.get(index + 1);
                        nextHeaderView = parent.getChildAt(firstChildIndex + (nextHeaderPos - firstChildPos));
                    }
                    int translationX = getTranslationX(parent, mStickyHeader.itemView, nextHeaderView);
                    int translationY = getTranslationY(parent, mStickyHeader.itemView, nextHeaderView);
                    onDisplayStickyHeader(mStickyHeader, parent, canvas, translationX, translationY);
                    return;
                }
            }
        }

        if (mStickyHeader != null) {
            scrapStickyHeader(parent);
        }
    }

    /**
     * Handles display and positioning of the sticky header. Invoked inside {@link View#draw(Canvas)}.
     */
    protected abstract void onDisplayStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent,
                                                  Canvas canvas, int translationX, int translationY);

    /**
     * Creates {@link RecyclerView.ViewHolder} for {@code position}, including measure / layout, and assigns it to
     * {@link #mStickyHeader}.
     */
    private void createStickyHeader(RecyclerView parent, int position) {
        int itemViewType = mAdapter.getItemViewType(position);

        // Check the pool to see if we can recycle a view.
        RecyclerView.RecycledViewPool pool = parent.getRecycledViewPool();
        if (pool != null) {
            mStickyHeader = pool.getRecycledView(itemViewType);
        }

        // Inflate one if needed.
        if (mStickyHeader == null) {
            mStickyHeader = mAdapter.createViewHolder(parent, itemViewType);
        }

        // Mark dirty as its not bound yet.
        mDirty = true;

        // Sticky header is created. Pass it on.
        onCreateStickyHeader(mStickyHeader, parent, position);
    }

    /**
     * Handles a created sticky header. Invoked inside {@link View#draw(Canvas)}.
     */
    protected abstract void onCreateStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent,
                                                 int position);

    /**
     * Binds the {@link #mStickyHeader} for the given {@code position}.
     */
    @SuppressWarnings("unchecked")
    private void bindStickyHeader(RecyclerView parent, int position) {
        // Bind the sticky header.
        mAdapter.bindViewHolder(mStickyHeader, position);
        mDirty = false;

        // Sticky header is bound. Pass it on.
        onBindStickyHeader(mStickyHeader, parent, position);
    }

    /**
     * Handles a bound sticky header. Invoked inside {@link View#draw(Canvas)}.
     */
    protected abstract void onBindStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent, int position);

    /**
     * Returns {@link #mStickyHeader} to the {@link RecyclerView}'s {@link RecyclerView.RecycledViewPool}, assigning it
     * to {@code null}.
     */
    private void scrapStickyHeader(final RecyclerView parent) {
        onScrapStickyHeader(mStickyHeader, parent);

        RecyclerView.RecycledViewPool pool = parent.getRecycledViewPool();
        if (pool != null) {
            pool.putRecycledView(mStickyHeader);
        }

        mStickyHeader = null;
        mDirty = false;
    }

    /**
     * Handles a sticky header that is about to be scrapped. Invoked inside {@link View#draw(Canvas)}.
     */
    protected abstract void onScrapStickyHeader(RecyclerView.ViewHolder stickyHeader, RecyclerView parent);

    /**
     * Returns true when the {@code view} is at the edge of the parent {@link RecyclerView}.
     */
    private boolean isViewOnBoundary(RecyclerView parent, View view) {
        if (isVertical(parent)) {
            if (isReverse(parent)) {
                return view.getBottom() > parent.getHeight();
            } else {
                return view.getTop() < 0;
            }
        } else {
            if (isReverse(parent)) {
                return view.getRight() > parent.getWidth();
            } else {
                return view.getLeft() < 0;
            }
        }
    }

    /**
     * Returns the necessary translation in the X axis to position the header appropriately, depending on orientation
     * and direction.
     */
    private int getTranslationX(RecyclerView parent, View headerView, View nextHeaderView) {
        if (!isVertical(parent)) {
            int translationX;
            if (isReverse(parent)) {
                translationX = parent.getWidth() - headerView.getWidth();
            } else {
                translationX = 0;
            }
            if (nextHeaderView != null) {
                if (isReverse(parent)) {
                    translationX = Math.max(nextHeaderView.getRight(), translationX);
                } else {
                    translationX = Math.min(nextHeaderView.getLeft() - headerView.getWidth(), translationX);
                }
            }
            return translationX;
        } else {
            return 0;
        }
    }

    /**
     * Returns the necessary translation in the Y axis to position the header appropriately, depending on orientation,
     * direction and {@link android.R.attr#clipToPadding}.
     */
    private int getTranslationY(RecyclerView parent, View headerView, View nextHeaderView) {
        if (isVertical(parent)) {
            int translationY;
            if (isReverse(parent)) {
                translationY = parent.getHeight() - headerView.getHeight();
            } else {
                translationY = 0;
            }
            if (nextHeaderView != null) {
                if (isReverse(parent)) {
                    translationY = Math.max(nextHeaderView.getBottom(), translationY);
                } else {
                    translationY = Math.min(nextHeaderView.getTop() - headerView.getHeight(), translationY);
                }
            }
            return translationY;
        } else {
            return 0;
        }
    }

    /**
     * Returns true if the layout is a vertical one.
     */
    protected boolean isVertical(RecyclerView parent) {
        if (mIsVertical != null) {
            return mIsVertical;
        } else {
            RecyclerView.LayoutManager manager = parent.getLayoutManager();
            return !(manager instanceof LinearLayoutManager)
                    || ((LinearLayoutManager) manager).getOrientation() == LinearLayoutManager.VERTICAL;
        }
    }

    /**
     * Returns true if the layout is in reverse.
     */
    protected boolean isReverse(RecyclerView parent) {
        if (mIsReverse != null) {
            return mIsReverse;
        } else {
            RecyclerView.LayoutManager manager = parent.getLayoutManager();
            return manager instanceof LinearLayoutManager && ((LinearLayoutManager) manager).getReverseLayout();
        }
    }

    /**
     * Given an ordered {@code list}, finds the position of {@code value} or the closest smaller value.
     */
    private int findOrderedIndex(List<Integer> list, int value) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            if (value < list.get(middle)) {
                high = middle - 1;
            } else if (middle < list.size() - 1 && value >= list.get(middle + 1)) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    /**
     * Given an ordered {@code list}, finds the position of {@code value} using binary search.
     */
    private int findIndex(List<Integer> list, int value) {
        int pos = findOrderedIndex(list, value);
        return pos != -1 && list.get(pos) == value ? pos : -1;
    }

    /**
     * Handles header information while adapter changes occur.
     */
    private class SectionInfoAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            // There's no hint at what changed, so go through the adapter.
            mHeaderPositions.clear();
            int itemCount = mAdapter.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                if (mAdapter.isHeader(i)) {
                    mHeaderPositions.add(i);
                }
            }

            // There's no way to know if the sticky header is dirty or not.
            mDirty = true;
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            if (mStickyHeader != null
                    && mStickyHeader.getPosition() >= positionStart
                    && mStickyHeader.getPosition() < positionStart + itemCount) {
                mDirty = true;
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            for (int i = positionStart; i < positionStart + itemCount; i++) {
                // Get the first section index affected by this insertion.
                int index = findIndex(mHeaderPositions, i);
                if (index == -1) {
                    index = findOrderedIndex(mHeaderPositions, i) + 1;
                }

                // If the added item itself is a header, add it to the list.
                if (mAdapter.isHeader(i)) {
                    mHeaderPositions.add(index, i);
                    index++; // Increment so that the following offset doesn't apply to the new header.
                }

                // Shift all affected headers given the newly inserted item.
                for (int j = index; j < mHeaderPositions.size(); j++) {
                    int headerPos = mHeaderPositions.get(j);
                    mHeaderPositions.set(j, headerPos + 1);

                    // Mark the sticky header as dirty if shifted.
                    if (mStickyHeader != null && headerPos == mStickyHeader.getPosition()) {
                        mDirty = true;
                    }
                }
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            // Remove all headers affected by this remove.
            for (int i = positionStart + itemCount - 1; i >= positionStart; i--) {
                int index = findIndex(mHeaderPositions, i);
                if (index != -1) {
                    int headerPos = mHeaderPositions.remove(index);

                    // Mark the sticky header as dirty if removed. There might be a different one in the same position.
                    if (mStickyHeader != null && headerPos == mStickyHeader.getPosition()) {
                        mDirty = true;
                    }
                }
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            // Gather start / end positions for an increasing for loop.
            int start;
            int end;
            if (fromPosition < toPosition) {
                start = fromPosition;
                end = toPosition;
            } else {
                start = toPosition;
                end = fromPosition + itemCount;
            }

            // Remove all sections affected by the move, and check the adapter to add them back in.
            int adapterItemCount = mAdapter.getItemCount();
            for (int i = start; i < end; i++) {
                int index = findIndex(mHeaderPositions, i);
                boolean isHeader = i < adapterItemCount && mAdapter.isHeader(i);
                if (index != -1) {
                    if (!isHeader) {
                        int headerPos = mHeaderPositions.remove(index);

                        // Mark the sticky header as dirty if removed.
                        if (mStickyHeader != null && headerPos == mStickyHeader.getPosition()) {
                            mDirty = true;
                        }
                    }
                } else if (isHeader) {
                    mHeaderPositions.add(findOrderedIndex(mHeaderPositions, i) + 1, i);
                }
            }
        }
    }
}
