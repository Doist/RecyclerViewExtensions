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
import java.util.Arrays;
import java.util.List;

/**
 * Adds sticky headers capabilities to your {@link RecyclerView.Adapter}. It must implement {@link StickyHeaders} to
 * indicate which items are headers.
 *
 * Handles most of the background workflow, but the display logic behind it can differ depending on the implementation:
 * 1. {@link StickyHeaderCanvasItemDecoration} is the fastest implementation. However, it doesn't support click
 * listeners or animations for the sticky headers. It draws the sticky header manually on the {@link Canvas}.
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
        adapter.registerAdapterDataObserver(new HeaderPositionsAdapterDataObserver());
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
                    firstChildPos = params.getViewLayoutPosition();
                    break;
                }
            }

            Log.e("WUT", "Header positions: " + Arrays.toString(mHeaderPositions.toArray()));

            if (firstChild != null) {
                int index = findHeaderIndexOrBefore(firstChildPos);
                int headerPos = index != -1 ? mHeaderPositions.get(index) : -1;
                // Show header if there's one to show, and if it's not the first view or is on the edge.

                if (headerPos != -1 && (headerPos != firstChildPos || isViewOnBoundary(parent, firstChild))) {
                    // Ensure existing sticky header, if any, is of correct type.
                    if (mStickyHeader != null
                            && mStickyHeader.getItemViewType() != mAdapter.getItemViewType(headerPos)) {
                        // A sticky header was setup before but is not of the correct type. Scrap it.
                        scrapStickyHeader(parent);
                    }

                    // Ensure sticky header is created, if absent, or bound, if dirty or the position changed.
                    if (mStickyHeader == null) {
                        createStickyHeader(parent, headerPos);
                    }
                    if (mDirty || mStickyHeader.getLayoutPosition() != headerPos) {
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
     * Finds the header index of {@code position} in {@code mHeaderPositions}.
     */
    private int findHeaderIndex(int position) {
        int low = 0;
        int high = mHeaderPositions.size() - 1;
        while (low <= high) {
            int middle = (low + high) / 2;
            if (mHeaderPositions.get(middle) > position) {
                high = middle - 1;
            } else if(mHeaderPositions.get(middle) < position) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    /**
     * Finds the header index of {@code position} or the one before it in {@code mHeaderPositions}.
     */
    private int findHeaderIndexOrBefore(int position) {
        int low = 0;
        int high = mHeaderPositions.size() - 1;
        while (low <= high) {
            int middle = (low + high) / 2;
            if (mHeaderPositions.get(middle) > position) {
                high = middle - 1;
            } else if (middle < mHeaderPositions.size() - 1 && mHeaderPositions.get(middle + 1) <= position) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    /**
     * Finds the header index of {@code position} or the one next to it in {@code mHeaderPositions}.
     */
    private int findHeaderIndexOrNext(int position) {
        int low = 0;
        int high = mHeaderPositions.size() - 1;
        while (low <= high) {
            int middle = (low + high) / 2;
            if (middle > 0 && mHeaderPositions.get(middle - 1) >= position) {
                high = middle - 1;
            } else if(mHeaderPositions.get(middle) < position) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    /**
     * Handles header positions while adapter changes occur.
     */
    private class HeaderPositionsAdapterDataObserver extends RecyclerView.AdapterDataObserver implements Runnable {
        @Override
        public void run() {
            int itemCount = mAdapter.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                if (mAdapter.isHeader(i)) {
                    mHeaderPositions.add(i);
                }
            }
        }

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

            // Mark current header as dirty if changed.
            dirtCurrentHeaderIfPositionChanged(null, null);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            // Mark current header as dirty if changed.
            dirtCurrentHeaderIfPositionChanged(positionStart, positionStart + itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            // Shift headers below down.
            int headerCount = mHeaderPositions.size();
            if (headerCount > 0) {
                for (int i = findHeaderIndexOrNext(positionStart); i != -1 && i < headerCount;  i++) {
                    mHeaderPositions.set(i, mHeaderPositions.get(i) + itemCount);
                }
            }

            // Add new headers.
            for (int i = positionStart; i < positionStart + itemCount; i++) {
                if (mAdapter.isHeader(i)) {
                    int headerIndex = findHeaderIndexOrNext(i);
                    if (headerIndex != -1) {
                        mHeaderPositions.add(headerIndex, i);
                    } else {
                        mHeaderPositions.add(i);
                    }
                }
            }

            // Mark current header as dirty if affected.
            dirtCurrentHeaderIfPositionChanged(positionStart, null);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            int headerCount = mHeaderPositions.size();
            if (headerCount > 0) {
                // Remove headers.
                for (int i = positionStart + itemCount - 1; i >= positionStart; i--) {
                    int index = findHeaderIndex(i);
                    if (index != -1) {
                        mHeaderPositions.remove(index);
                        headerCount--;
                    }
                }

                // Shift headers below up.
                for (int i = findHeaderIndexOrNext(positionStart + itemCount); i != -1 && i < headerCount; i++) {
                    mHeaderPositions.set(i, mHeaderPositions.get(i) - itemCount);
                }
            }

            // Mark current header as dirty if changed.
            dirtCurrentHeaderIfPositionChanged(positionStart, null);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            // Shift moved headers by toPosition - fromPosition.
            // Shift headers in-between by -itemCount (reverse if upwards).
            int headerCount = mHeaderPositions.size();
            if (headerCount > 0) {
                if (fromPosition < toPosition) {
                    for (int i = findHeaderIndexOrNext(fromPosition); i != -1 && i < headerCount; i++) {
                        int headerPos = mHeaderPositions.get(i);
                        if (headerPos >= fromPosition && headerPos < fromPosition + itemCount) {
                            mHeaderPositions.set(i, headerPos + (toPosition - fromPosition));
                            sortHeaderAtIndex(i);
                        } else if (headerPos >= fromPosition + itemCount && headerPos < toPosition) {
                            mHeaderPositions.set(i, headerPos - itemCount);
                            sortHeaderAtIndex(i);
                        } else if (headerPos > toPosition) {
                            break;
                        }
                    }
                } else {
                    for (int i = findHeaderIndexOrNext(toPosition); i != -1 && i < headerCount; i++) {
                        int headerPos = mHeaderPositions.get(i);
                        if (headerPos >= fromPosition && headerPos < fromPosition + itemCount) {
                            mHeaderPositions.set(i, headerPos + (toPosition - fromPosition));
                            sortHeaderAtIndex(i);
                        } else if (headerPos >= toPosition && headerPos < fromPosition) {
                            mHeaderPositions.set(i, headerPos + itemCount);
                            sortHeaderAtIndex(i);
                        } else if (headerPos > toPosition) {
                            break;
                        }
                    }
                }
            }

            // Mark current header as dirty if changed.
            dirtCurrentHeaderIfPositionChanged(Math.min(fromPosition, toPosition),
                                               Math.max(fromPosition, toPosition) + itemCount);
        }

        private void sortHeaderAtIndex(int index) {
            int headerPos = mHeaderPositions.remove(index);
            int headerIndex = findHeaderIndexOrNext(headerPos);
            if (headerIndex != -1) {
                mHeaderPositions.add(headerIndex, headerPos);
            } else {
                mHeaderPositions.add(headerPos);
            }
        }

        /**
         * Marks the current header as dirty if it's within the start / end range (ie. its position changed) and there
         * is another one in its previous position.
         */
        private void dirtCurrentHeaderIfPositionChanged(Integer rangeStart, Integer rangeEnd) {
            if (mStickyHeader != null) {
                if ((rangeStart == null && rangeEnd == null)
                        || ((rangeStart == null || mStickyHeader.getLayoutPosition() >= rangeStart)
                        && (rangeEnd == null || mStickyHeader.getLayoutPosition() < rangeEnd))) {
                    if (findHeaderIndex(mStickyHeader.getLayoutPosition()) != -1) {
                        mDirty = true;
                    }
                }
            }
        }
    }
}
