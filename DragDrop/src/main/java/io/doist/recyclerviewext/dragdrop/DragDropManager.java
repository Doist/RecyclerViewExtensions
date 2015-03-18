package io.doist.recyclerviewext.dragdrop;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Adds drag and drop abilities to your {@link RecyclerView} and its {@link RecyclerView.Adapter}.
 *
 * The workflow is as follows:
 * 1. Have the adapter implement {@link DragDrop}
 * 2. Call {@link #start(int)} for the position to be dragged when there is an ongoing touch event, which will be
 * tracked
 * 3. Your adapter's {@link DragDrop#moveItem(int, int)} is called when the user releases the item
 *
 * At any time, {@link #stop()} or {@link #cancel()} can be called to either stop or cancel the drag and drop operation.
 * For optimal results, your {@link RecyclerView.Adapter} should have stable ids, but this is not mandatory.
 *
 * The logic itself is based on an {@link RecyclerView.OnItemTouchListener}, which monitors touch events, an
 * {@link RecyclerView.ItemDecoration}, which draws the dragged item on top of the {@link RecyclerView}, and a wrapper
 * adapter that facilitates moving the dragged item around before submitting. The {@link RecyclerView}'s
 * {@link RecyclerView.ItemAnimator} is used for swapping items during drag and drop, providing enough flexibility to
 * easily use custom move animations.
 */
public class DragDropManager<VH extends RecyclerView.ViewHolder, T extends RecyclerView.Adapter<VH> & DragDrop>
        extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {
    public static final String LOG_TAG = DragDropManager.class.getSimpleName();

    private static final float SCROLL_SPEED_MAX_DP = 16;
    private static final int SETTLE_DURATION_MS = 250;

    private RecyclerView mRecyclerView;
    private T mAdapter;

    /**
     * Whether the user is currently dragging an item or not.
     * {@code true} as soon as {@link #start(int)} is called (with an ongoing touch event),
     * {@code false} as soon as the touch event ends and settling into the final position starts.
     */
    private boolean mDragging;

    /**
     * Position of the dragged item in the original adapter. It's updated by {@link DragDropAdapter} if the underlying
     * adapter changes.
     */
    private int mItemPosition;
    /**
     * Used to draw the item. Updates can be requested by {@link DragDropAdapter} when the underlying item changes.
     * Due to lack of support for z-order on KitKat and before the view itself is not used, as it'd be clipped by the
     * following view while dragging.
     */
    private Bitmap mItemBitmap;
    /**
     * Item location on screen, updated in every {@link #onDrawOver(Canvas, RecyclerView, RecyclerView.State)} cycle.
     */
    private Rect mItemLocation = new Rect();

    private int mItemInitialLeft;
    private int mItemInitialTop;

    private int mTouchStartX;
    private int mTouchStartY;
    private int mTouchCurrentX;
    private int mTouchCurrentY;
    private boolean mTouchUpToDate;

    private int mLayoutOrientation;

    private final float mScrollSpeedMax;
    private float mScrollSpeed = 0f;

    private FindPositionRunnable mFindPositionRunnable = new FindPositionRunnable();
    private SettlePositionRunnable mSettlePositionRunnable = new SettlePositionRunnable();

    /**
     * Wrapper adapter that facilitates drag and drop by wrapping another adapter and keeping track of the original and
     * current positions for the dragged item.
     */
    private DragDropAdapter mDragDropAdapter;

    public DragDropManager(RecyclerView recyclerView, T adapter) {
        mRecyclerView = recyclerView;
        mAdapter = adapter;

        mRecyclerView.addOnItemTouchListener(this);

        Resources res = recyclerView.getResources();
        mScrollSpeedMax =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SCROLL_SPEED_MAX_DP, res.getDisplayMetrics());
    }

    /**
     * Starts drag and drop for the item in {@code position}. There must be an ongoing touch event, either because the
     * user long pressed on the item or touched its drag handle.
     *
     * @return {@code true} if the drag and drop started successfully, {@code false} if not.
     */
    public boolean start(int position) {
        if (mTouchUpToDate) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForLayoutPosition(position);
            if (holder != null && holder.itemView != null) {
                mDragging = true;

                mItemPosition = position;

                int itemLeft = holder.itemView.getLeft();
                int itemTop = holder.itemView.getTop();

                // Grab the first location using this item's view.
                mItemLocation.set(itemLeft, itemTop, holder.itemView.getRight(), holder.itemView.getBottom());

                // Store the initial left / top of the item to calculate left / top limits during the drag.
                mItemInitialLeft = itemLeft;
                mItemInitialTop = itemTop;

                // Create the item bitmap to be drawn in onDrawOver().
                updateItemBitmap(holder);

                // Swap the adapter with a wrapper adapter that facilitates drag and drop by moving the dragged position
                // around while mapping to the original adapter's positions.
                mDragDropAdapter = new DragDropAdapter<>(this, mAdapter, position);
                mRecyclerView.swapAdapter(mDragDropAdapter, false);

                // Add item decoration to draw the dragged item. Also invalidates the RecyclerView, which calls
                // onDrawOver() which draws the first frame.
                mRecyclerView.addItemDecoration(this);

                return true;
            } else {
                Log.w(LOG_TAG, "There is no ongoing touch event.");
            }
        }

        return false;
    }

    /**
     * Stops the current drag and drop, submitting the current position and animating the item into place.
     * To cancel, use {@link #cancel()}.
     *
     * @return {@code true} if the drag and drop stopped successfully, {@code false} if it hasn't been started.
     */
    public boolean stop() {
        if (mDragging) {
            mDragging = false;

            // Commit the move tracked so far by the wrapper adapter.
            mDragDropAdapter.commitCurrentPosition();

            // Settle the item bitmap in its final location. When done, call cleanupInternal() which will release the
            // remaining resources and effectively stop the drag and drop.
            mSettlePositionRunnable.start(
                    mDragDropAdapter.getCurrentPosition(),
                    new Runnable() {
                        @Override
                        public void run() {
                            cleanupInternal();
                        }
                    });

            return true;
        }

        return false;
    }

    /**
     * Cancels the current drag and drop immediately.
     *
     * @return {@code true} if the drag and drop was cancelled successfully, {@code false} if it hasn't been started.
     */
    public boolean cancel() {
        if (mDragging) {
            mDragging = false;

            cleanupInternal();

            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void cleanupInternal() {
        // Clear the item bitmap.
        if (mItemBitmap != null) {
            mItemBitmap.recycle();
            mItemBitmap = null;
        }

        // Remove item decoration as the item bitmap will no longer be drawn.
        mRecyclerView.removeItemDecoration(this);

        final RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();

        // Undo any changes made by the wrapper adapter or animations on the dragged item, ensuring it's returned to the
        // view pool in a sane and usable state.
        VH holder = (VH) mRecyclerView.findViewHolderForLayoutPosition(mDragDropAdapter.getCurrentPosition());
        if (!holder.isRecyclable()) {
            mDragDropAdapter.onFailedToRecycleView(holder);
            if (itemAnimator != null) {
                itemAnimator.endAnimation(holder);
            }
        }

        // Swap wrapper adapter with the original one. The item move, if submitted, has been done in stop().
        mRecyclerView.swapAdapter(mAdapter, false);
        mDragDropAdapter.destroy();
        mDragDropAdapter = null;

        // Ensure no item animations triggered by swapping adapters run.
        if (itemAnimator != null) {
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    itemAnimator.endAnimations();
                }
            });
        }

        // Reset the scroll speed.
        mScrollSpeed = 0;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        // Despite returning true the first time, the next event still comes here, hence the apparently unecessary
        // handling for ACTION_MOVE, ACTION_UP and ACTION_CANCEL.

        int action = MotionEventCompat.getActionMasked(e);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleDown(e);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mDragging) {
                    handleMove(e);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mDragging) {
                    handleUp();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mDragging) {
                    handleCancel();
                }
                break;
        }
        return mDragging;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        final int action = MotionEventCompat.getActionMasked(e);

        if (!mDragging) {
            return;
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                handleMove(e);
                break;

            case MotionEvent.ACTION_UP:
                handleUp();
                break;

            case MotionEvent.ACTION_CANCEL:
                handleCancel();
                break;
        }
    }

    private void handleDown(MotionEvent e) {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            throw new UnsupportedOperationException("DragDropManager only supports LinearLayoutManager");
        }
        mLayoutOrientation = ((LinearLayoutManager) layoutManager).getOrientation();

        // Grab the starting touch position to calculate the offset later.
        mTouchStartX = mTouchCurrentX = (int) e.getX();
        mTouchStartY = mTouchCurrentY = (int) e.getY();
        mTouchUpToDate = true;

        // If dragging, invalidate the location where the item is being drawn.
        if (mDragging) {
            mRecyclerView.invalidate(mItemLocation);
        }
    }

    private void handleMove(MotionEvent e) {
        if (mLayoutOrientation == LinearLayoutManager.HORIZONTAL) {
            mTouchCurrentX = (int) e.getX();
        } else {
            mTouchCurrentY = (int) e.getY();
        }
        mTouchUpToDate = true;

        // If dragging, invalidate the location where the item is being drawn.
        if (mDragging) {
            mRecyclerView.invalidate(mItemLocation);
        }
    }

    private void handleUp() {
        mTouchUpToDate = false;

        stop();
    }

    private void handleCancel() {
        mTouchUpToDate = false;

        cancel();
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 0);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        if (mItemBitmap != null) {
            // Update item location if dragging. Otherwise it's settling and the position is updated externally.
            if (mDragging) {
                updateItemLocation();
            }

            // Draw item bitmap.
            c.drawBitmap(mItemBitmap, mItemLocation.left, mItemLocation.top, null);

            // Swap with adjacent items if dragging.
            if (mDragging) {
                handleSwap();
            }

            // Handle scrolling when on the edges.
            handleScroll(!mDragging);
        }
    }

    /**
     * Updates the item bitmap draw location using the touch position relative to start and the boundaries imposed
     * by both the {@link RecyclerView} and the specified {@link DragDrop#getDragStartBoundaryPosition(int)} and
     * {@link DragDrop#getDragEndBoundaryPosition(int)} in the adapter.
     */
    private void updateItemLocation() {
        int minLeft = mRecyclerView.getPaddingLeft();
        int maxLeft = mRecyclerView.getWidth() - mRecyclerView.getPaddingLeft() - mItemBitmap.getWidth();
        int minTop = mRecyclerView.getPaddingTop();
        int maxTop = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom() - mItemBitmap.getHeight();

        int startBoundaryPosition = mDragDropAdapter.getStartBoundaryPosition();
        if (startBoundaryPosition != DragDrop.NO_BOUNDARY) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForLayoutPosition(startBoundaryPosition);
            if (holder != null) {
                if (mLayoutOrientation == LinearLayoutManager.HORIZONTAL) {
                    minLeft = Math.max(minLeft, holder.itemView.getRight());
                } else {
                    minTop = Math.max(minTop, holder.itemView.getBottom());
                }
            }
        }
        int endBoundaryPosition = mDragDropAdapter.getEndBoundaryPosition();
        Log.e("WUT", "Current position: " + mDragDropAdapter.getCurrentPosition() + ", end boundary position: " + endBoundaryPosition);
        if (endBoundaryPosition != DragDrop.NO_BOUNDARY && mDragDropAdapter.getCurrentPosition() < endBoundaryPosition) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForLayoutPosition(endBoundaryPosition);
            if (holder != null) {
                if (mLayoutOrientation == LinearLayoutManager.HORIZONTAL) {
                    maxLeft = Math.min(maxLeft, holder.itemView.getLeft() - mItemBitmap.getWidth());
                } else {
                    maxTop = Math.min(maxTop, holder.itemView.getTop() - mItemBitmap.getHeight());
                }
            }
        }

        int left = Math.max(minLeft, Math.min(maxLeft, mItemInitialLeft + (mTouchCurrentX - mTouchStartX)));
        int top = Math.max(minTop, Math.min(maxTop, mItemInitialTop + (mTouchCurrentY - mTouchStartY)));
        mItemLocation.offsetTo(left, top);
    }

    /**
     * Scrolls the {@link RecyclerView} when the item bitmap is drawn close to the edges.
     * The scroll starts when the item bitmap is {@code itemWidth} / {@code itemHeight} away from the edge.
     *
     * @param decelerate if {@code true}, the scroll is decelerated regardless of the proximity to the edges.
     */
    private void handleScroll(boolean decelerate) {
        int scrollByX = 0;
        int scrollByY = 0;
        int direction;

        if (mLayoutOrientation == LinearLayoutManager.HORIZONTAL) {
            float itemCenterX = (mItemLocation.left + mItemLocation.right) / 2f;
            direction = itemCenterX < mRecyclerView.getWidth() / 2f ? -1 : 1;
            if (canScrollHorizontally(direction)) {
                float itemWidth = mItemLocation.right - mItemLocation.left;
                if (direction == -1) {
                    int left = mRecyclerView.getPaddingLeft();
                    float boundaryLeft = left + itemWidth;
                    if (!decelerate && mItemLocation.left < boundaryLeft) {
                        updateScrollSpeedAccelerating(1 - ((mItemLocation.left - left) / (boundaryLeft - left)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                } else {
                    int right = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight();
                    float boundaryRight = right - itemWidth;
                    if (!decelerate && mItemLocation.right > boundaryRight) {
                        updateScrollSpeedAccelerating(1 - ((right - mItemLocation.right) / (right - boundaryRight)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                }
                scrollByX = (int) mScrollSpeed;
            } else if (mScrollSpeed > 0) {
                scrollByX = (int) mScrollSpeed; // One last scroll to show edge glow.
                mScrollSpeed = 0;
            }
        } else {
            float itemCenterY = (mItemLocation.top + mItemLocation.bottom) / 2f;
            direction = itemCenterY < mRecyclerView.getHeight() / 2f ? -1 : 1;
            if (canScrollVertically(direction)) {
                float itemHeight = mItemLocation.bottom - mItemLocation.top;
                if (direction == -1) {
                    int top = mRecyclerView.getPaddingTop();
                    float boundaryTop = top + itemHeight;
                    if (!decelerate && mItemLocation.top < boundaryTop) {
                        updateScrollSpeedAccelerating(1 - ((mItemLocation.top - top) / (boundaryTop - top)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                } else {
                    int bottom = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom();
                    float boundaryBottom = bottom - itemHeight;
                    if (!decelerate && mItemLocation.bottom > boundaryBottom) {
                        updateScrollSpeedAccelerating(
                                1 - ((bottom - mItemLocation.bottom) / (bottom - boundaryBottom)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                }
                scrollByY = (int) mScrollSpeed;
            } else if (mScrollSpeed > 0) {
                scrollByY = (int) mScrollSpeed; // One last scroll to show edge glow.
                mScrollSpeed = 0;
            }
        }

        if (scrollByX > 0 || scrollByY > 0) {
            mRecyclerView.scrollBy(scrollByX * direction, scrollByY * direction);
        }
    }

    /**
     * Alternative to {@link RecyclerView#canScrollVertically(int)} that is considerably faster and more suited to use
     * while drawing. Doesn't compute scroll offsets, ranges or extents, and just relies on the child views.
     */
    private boolean canScrollVertically(int direction) {
        int childCount = mRecyclerView.getChildCount();
        if (childCount > 0) {
            RecyclerView.ViewHolder holder;
            if (direction < 0) {
                holder = mRecyclerView.findViewHolderForLayoutPosition(0);
                if (holder != null) {
                    int minTop = Integer.MAX_VALUE;
                    for (int i = 0; i < childCount; i++) {
                        minTop = Math.min(minTop, mRecyclerView.getChildAt(i).getTop());
                    }
                    return minTop < mRecyclerView.getPaddingTop();
                } else {
                    return true;
                }
            } else {
                holder = mRecyclerView.findViewHolderForLayoutPosition(mAdapter.getItemCount() - 1);
                if (holder != null) {
                    int maxBottom = Integer.MIN_VALUE;
                    for (int i = 0; i < childCount; i++) {
                        maxBottom = Math.max(maxBottom, mRecyclerView.getChildAt(i).getBottom());
                    }
                    return maxBottom > mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom();
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Alternative to {@link RecyclerView#canScrollHorizontally(int)} that is considerably faster and more suited to use
     * while drawing. Doesn't compute scroll offsets, ranges or extents, and just relies on the child views.
     */
    private boolean canScrollHorizontally(int direction) {
        int childCount = mRecyclerView.getChildCount();
        if (childCount > 0) {
            RecyclerView.ViewHolder holder;
            if (direction < 0) {
                holder = mRecyclerView.findViewHolderForLayoutPosition(0);
                if (holder != null) {
                    int minLeft = Integer.MAX_VALUE;
                    for (int i = 0; i < childCount; i++) {
                        minLeft = Math.min(minLeft, mRecyclerView.getChildAt(i).getLeft());
                    }
                    return minLeft < mRecyclerView.getPaddingRight();
                } else {
                    return true;
                }
            } else {
                holder = mRecyclerView.findViewHolderForLayoutPosition(mAdapter.getItemCount() - 1);
                if (holder != null) {
                    int maxRight = Integer.MIN_VALUE;
                    for (int i = 0; i < childCount; i++) {
                        maxRight = Math.max(maxRight, mRecyclerView.getChildAt(i).getRight());
                    }
                    return maxRight > mRecyclerView.getWidth() - mRecyclerView.getPaddingRight();
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates the scroll speed for an accelerating motion, until {@code mScrollSpeedMax} is reached.
     *
     * @param scrollSpeedPercent How much of the calculated scroll speed should be applied. Depends on how close to the
     *                           boundary the item bitmap location is. [0..1].
     */
    private void updateScrollSpeedAccelerating(float scrollSpeedPercent) {
        if (mScrollSpeed < 1) {
            mScrollSpeed = 1;
        } else if (mScrollSpeed < mScrollSpeedMax) {
            mScrollSpeed = Math.min(Math.max(1, mScrollSpeedMax * scrollSpeedPercent), mScrollSpeed * 1.075f);
        }
    }

    /**
     * Updates the scroll speed for a decelerating motion, until {@code 0} is reached.
     */
    private void updateScrollSpeedDecelerating() {
        if (mScrollSpeed > 1) {
            mScrollSpeed = mScrollSpeed / 1.2f;
        } else if(mScrollSpeed > 0) {
            mScrollSpeed = 0;
        }
    }

    /**
     * Swaps the dragged position in the wrapper adapter whenever it overlaps at least half of another position.
     */
    private void handleSwap() {
        View swapView = null;
        boolean mightHaveWrongAnchorView = false;

        if (mLayoutOrientation == LinearLayoutManager.HORIZONTAL) {
            View leftView = findChildViewUnder(mItemLocation.left, mItemLocation.centerY());
            if (leftView != null && mItemLocation.left < (leftView.getLeft() + leftView.getRight()) / 2f) {
                swapView = leftView;
                mightHaveWrongAnchorView = true;
            } else {
                View rightView = findChildViewUnder(mItemLocation.right, mItemLocation.centerY());
                if (rightView != null && mItemLocation.right > (rightView.getLeft() + rightView.getRight()) / 2f) {
                    swapView = rightView;
                }
            }
        } else {
            View topView = findChildViewUnder(mItemLocation.centerX(), mItemLocation.top);
            if (topView != null && mItemLocation.top < (topView.getTop() + topView.getBottom()) / 2f) {
                swapView = topView;
                mightHaveWrongAnchorView = true;
            } else {
                View bottomView = findChildViewUnder(mItemLocation.centerX(), mItemLocation.bottom);
                if (bottomView != null && mItemLocation.bottom > (bottomView.getTop() + bottomView.getBottom()) / 2f) {
                    swapView = bottomView;
                }
            }
        }

        if (swapView != null) {
            int swapPosition = mRecyclerView.getChildLayoutPosition(swapView);
            if (swapPosition != RecyclerView.NO_POSITION) {
                mDragDropAdapter.setCurrentPosition(swapPosition);

                // FIXME: https://code.google.com/p/android/issues/detail?id=99047
                if (mightHaveWrongAnchorView && (mRecyclerView.getChildAt(0) == swapView || mScrollSpeed > 0)) {
                    mRecyclerView.getLayoutManager().scrollToPosition(swapPosition);
                }
            }
        }
    }

    /**
     * Similar to {@link RecyclerView#findChildViewUnder(float, float)}, but only considers visible children and
     * disregards translation values.
     */
    private View findChildViewUnder(float x, float y) {
        RecyclerView.LayoutManager manager = mRecyclerView.getLayoutManager();
        final int count = manager.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = manager.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE
                    && x >= child.getLeft() && x <= child.getRight()
                    && y >= child.getTop() && y <= child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    /**
     * Updates the item bitmap. Besides the initial creation, it's used by {@link DragDropAdapter} whenever the dragged
     * item changes.
     */
    void updateItemBitmap(RecyclerView.ViewHolder holder) {
        int draggedItemWidth = holder.itemView.getWidth();
        int draggedItemHeight = holder.itemView.getHeight();

        if (mItemBitmap != null) {
            if (mItemBitmap.getWidth() == draggedItemWidth && mItemBitmap.getHeight() == draggedItemHeight) {
                mItemBitmap.eraseColor(Color.TRANSPARENT);
            } else {
                mItemBitmap.recycle();
                mItemBitmap = null;
            }
        }
        if (mItemBitmap == null) {
            mItemBitmap = Bitmap.createBitmap(draggedItemWidth, draggedItemHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(mItemBitmap);
        holder.itemView.draw(canvas);
    }

    /**
     * Updates the item bitmap in the next message queue loop using the {@link RecyclerView.ViewHolder} in the position
     * returned by {@link DragDropAdapter#getCurrentPosition()}.
     */
    void postUpdateItemBitmap() {
        mRecyclerView.post(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                int position = mDragDropAdapter.getCurrentPosition();
                RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForLayoutPosition(position);
                if (holder != null) {
                    mDragDropAdapter.bindViewHolder(holder, position);
                    updateItemBitmap(holder);
                }
            }
        });
    }

    /**
     * Updates the item position. Used by {@link DragDropAdapter} whenever the item position in the original adapter
     * changes.
     */
    void updateItemPosition(int position) {
        mItemPosition = position;
        if (!mFindPositionRunnable.isScheduled()) {
            mRecyclerView.postOnAnimation(mFindPositionRunnable);
            mFindPositionRunnable.setScheduled();
        }
    }

    /**
     * Finds the appropriate adapter position for {@link #mItemLocation} and sets it. Useful for positioning after the
     * original adapter changes.
     */
    private class FindPositionRunnable implements Runnable {
        private boolean mScheduled = false;

        @Override
        public void run() {
            mScheduled = false;

            int position = RecyclerView.NO_POSITION;
            View view = findChildViewUnder(mItemLocation.centerX(), mItemLocation.centerY());
            if (view != null) {
                position = mRecyclerView.getChildLayoutPosition(view);
            }
            if (position == RecyclerView.NO_POSITION) {
                position = mAdapter.getItemCount() - 1;
            }
            mDragDropAdapter.setCurrentPosition(position);
        }

        public boolean isScheduled() {
            return mScheduled;
        }

        public void setScheduled() {
            mScheduled = true;
        }
    }

    /**
     * Updates {@link #mItemLocation} for a settling animation into {@code mPosition}.
     */
    private class SettlePositionRunnable implements Runnable {
        private int mPosition;
        private Runnable mEndCallback;

        private int mStartLeft;
        private int mStartTop;

        private long mStartTime = 0;

        private Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

        @Override
        public void run() {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(mPosition);
            float fraction = (SystemClock.uptimeMillis() - mStartTime) / (float) SETTLE_DURATION_MS;
            if (holder != null && fraction < 1f) {
                int oldLeft = mItemLocation.left;
                int oldTop = mItemLocation.top;
                int oldRight = mItemLocation.right;
                int oldBottom = mItemLocation.bottom;

                View view = holder.itemView;
                float interpolatedFraction = mInterpolator.getInterpolation(fraction);
                int left = (int) (mStartLeft + (view.getLeft() - mStartLeft) * interpolatedFraction);
                int top = (int) (mStartTop + (view.getTop() - mStartTop) * interpolatedFraction);
                mItemLocation.offsetTo(left, top);

                mRecyclerView.invalidate(oldLeft, oldTop, oldRight, oldBottom);
                mRecyclerView.postOnAnimation(this);
            } else {
                mEndCallback.run();
            }
        }

        public void start(int position, Runnable endCallback) {
            mPosition = position;
            mStartLeft = mItemLocation.left;
            mStartTop = mItemLocation.top;
            mEndCallback = endCallback;

            mStartTime = SystemClock.uptimeMillis();
            mRecyclerView.invalidate(mItemLocation);
            mRecyclerView.postOnAnimation(this);
        }
    }
}