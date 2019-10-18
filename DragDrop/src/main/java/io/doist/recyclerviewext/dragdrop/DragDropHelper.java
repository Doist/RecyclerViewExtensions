package io.doist.recyclerviewext.dragdrop;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class for adding drag & drop to {@link RecyclerView}.
 *
 * It works with {@link RecyclerView}, {@link LinearLayoutManager} and a {@link Callback}.
 *
 * Call {@link #start(int)} during or right before a motion event to start dragging that specific position.
 * Call {@link #stop()} to stop the ongoing drag at the current position.
 */
public class DragDropHelper extends RecyclerView.ItemDecoration
        implements RecyclerView.OnItemTouchListener, RecyclerView.ChildDrawingOrderCallback {
    public static final String LOG_TAG = DragDropHelper.class.getSimpleName();

    private static final float SCROLL_SPEED_MAX_DP = 16;

    /**
     * No drag is ongoing.
     * Next state is {@link #STATE_STARTING}.
     */
    private static final int STATE_NONE = 0;
    /**
     * A drag is starting as soon as a touch event is received.
     * Next state is {@link #STATE_TRACKING} or {@link #STATE_DRAGGING}.
     */
    private static final int STATE_STARTING = 1;
    /**
     * A drag is ongoing without visual changes (ie. no translation, scrolling, position swap, etc).
     * Next state is {@link #STATE_RECOVERING} or {@link #STATE_DRAGGING}.
     */
    private static final int STATE_TRACKING = 2;
    /**
     * A drag is ongoing.
     * Next state is {@link #STATE_RECOVERING} or {@link #STATE_TRACKING}.
     */
    private static final int STATE_DRAGGING = 3;
    /**
     * A drag has ended and the state is recovering, ending animations are running.
     * Next state is {@link #STATE_STOPPING}.
     */
    private static final int STATE_RECOVERING = 4;
    /**
     * A drag has ended and all ending animations have run.
     * Next state is {@link #STATE_NONE}.
     */
    private static final int STATE_STOPPING = 5;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private Callback mCallback;

    /**
     * The position/view holder currently being dragged.
     */
    private Tracker mTracker;

    /**
     * Current state.
     * Can be {@link #STATE_NONE}, {@link #STATE_STARTING}, {@link #STATE_TRACKING}, {@link #STATE_DRAGGING}
     * or {@link #STATE_RECOVERING}.
     */
    private int mState = STATE_NONE;

    /**
     * Dragged item's location on screen.
     */
    private final Rect mLocation = new Rect();
    private int mStartLeft;
    private int mStartTop;

    /**
     * Starting coordinates for the touch event.
     */
    private int mTouchStartX;
    private int mTouchStartY;

    /**
     * Current coordinates for the touch event.
     */
    private int mTouchCurrentX;
    private int mTouchCurrentY;

    /**
     * Pivot coordinates for the touch event,
     * updated when the touch wanders more than {@link #mTouchSlop} in a direction.
     */
    private int mTouchPivotX;
    private int mTouchPivotY;

    /**
     * Touch direction in each coordinate.
     */
    private float mTouchDirectionX;
    private float mTouchDirectionY;

    private int mTouchSlop;

    private float mScrollSpeed;
    private float mScrollSpeedMax;

    /**
     * Attaches {@link DragDropHelper} to {@code recyclerView}. If already attached to a {@link RecyclerView}, it
     * detaches from the previous one. If {@code null} is provided, it detaches from the current {@link RecyclerView}.
     *
     * {@link DragDropHelper} uses {@link RecyclerView.ItemDecoration}, {@link RecyclerView.OnItemTouchListener} and
     * {@link RecyclerView.AdapterDataObserver} internally, the former 2 are set here.
     */
    public void attach(@Nullable RecyclerView recyclerView, @NonNull Callback callback) {
        if (mRecyclerView != recyclerView) {
            if (mRecyclerView != null) {
                mRecyclerView.removeItemDecoration(this);
                mRecyclerView.removeOnItemTouchListener(this);
            }
            mRecyclerView = recyclerView;
            if (mRecyclerView != null) {
                mScrollSpeedMax = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, SCROLL_SPEED_MAX_DP,
                        mRecyclerView.getResources().getDisplayMetrics());
                mTouchSlop = ViewConfiguration.get(recyclerView.getContext()).getScaledTouchSlop();
                mRecyclerView.addItemDecoration(this, 0);
                mRecyclerView.addOnItemTouchListener(this);
            }
        }
        mCallback = callback;
    }

    /**
     * Starts drag and drop for {@code position} as soon as there is a touch event,
     * or immediately if one is in progress.
     *
     * @return {@code true} if drag is starting, {@code false} if not.
     */
    public boolean start(int position) {
        if (mTracker != null && mTracker.getPosition() == position) {
            Log.w(LOG_TAG, "Position is already being dragged");
            return false;
        }
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
        if (holder == null) {
            Log.w(LOG_TAG, "Can't find view holder for position");
            return false;
        }
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            Log.w(LOG_TAG, "RecyclerView is not using LinearLayoutManager");
            return false;
        }
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter == null) {
            Log.w(LOG_TAG, "RecyclerView has no adapter");
            return false;
        }

        if (mState == STATE_STOPPING) {
            Log.w(LOG_TAG, "A drag is currently being stopped");
            return false;
        } else if (mState != STATE_NONE) {
            // Stop current drag immediately before starting new one.
            stop(true);
        }

        mLayoutManager = (LinearLayoutManager) layoutManager;
        mAdapter = adapter;

        // Grab position and holder.
        mTracker = new Tracker(position);
        mAdapter.registerAdapterDataObserver(mTracker);
        RecyclerView.ViewHolder viewHolder = mTracker.createViewHolder();

        // Get the initial location.
        mStartLeft = viewHolder.itemView.getLeft();
        mStartTop = viewHolder.itemView.getTop();
        mLocation.set(mStartLeft, mStartTop, viewHolder.itemView.getRight(), viewHolder.itemView.getBottom());

        // Drag will start.
        mState = STATE_STARTING;
        return true;
    }

    private void startInternal() {
        // Prevent ancestors from intercepting touch events.
        ViewParent recyclerViewParent = mRecyclerView.getParent();
        if (recyclerViewParent != null) {
            recyclerViewParent.requestDisallowInterceptTouchEvent(true);
        }

        // Add this as the child drawing order callback to ensure the dragged item is always on top.
        mRecyclerView.setChildDrawingOrderCallback(this);

        // Update state.
        mState = STATE_TRACKING;
    }

    private void moveInternal(int x, int y) {
        RecyclerView.ViewHolder viewHolder = mTracker.getViewHolder();
        if (viewHolder == null) {
            return;
        }
        int state = mCallback.onDragMoved(viewHolder, x, y) ? STATE_DRAGGING : STATE_TRACKING;
        if (mState == STATE_DRAGGING && state == STATE_TRACKING) {
            recoverInternal(viewHolder, mRecyclerView.getItemAnimator());
            mCallback.onDragStopped(viewHolder, false);
        } else if (mState == STATE_TRACKING && state == STATE_DRAGGING) {
            mCallback.onDragStarted(viewHolder, false);
        }
        mState = state;
    }

    /**
     * Stops the current drag and drop, animating the item into its final position.
     */
    public void stop() {
        stop(false);
    }

    private void stop(boolean now) {
        if (mState != STATE_NONE && mState != STATE_STOPPING) {
            RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
            RecyclerView.ViewHolder viewHolder = mTracker.getViewHolder();
            if (!now && mState == STATE_DRAGGING && viewHolder != null && itemAnimator != null) {
                recoverInternal(viewHolder, itemAnimator);

                // Stop internally after animations are done.
                itemAnimator.isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        stopInternal();
                    }
                });
            } else {
                if (mState == STATE_RECOVERING && viewHolder != null && itemAnimator != null) {
                    itemAnimator.endAnimations();
                } else {
                    stopInternal();
                }
            }
        }
    }

    private void stopInternal() {
        // Postpone stopping if a layout is being computed.
        mState = STATE_STOPPING;
        if (mRecyclerView.isComputingLayout()) {
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    stopInternal();
                }
            });
            return;
        }

        mAdapter.unregisterAdapterDataObserver(mTracker);
        mAdapter = null;
        mLayoutManager = null;

        mRecyclerView.setChildDrawingOrderCallback(null);
        mRecyclerView.invalidateItemDecorations();

        mState = STATE_NONE;
        mScrollSpeed = 0;

        mTracker.destroyViewHolder();
        mTracker = null;
    }

    private void recoverInternal(
            @NonNull RecyclerView.ViewHolder viewHolder, @Nullable RecyclerView.ItemAnimator itemAnimator) {
        mState = STATE_RECOVERING;

        // Setup preInfo with current translation values.
        RecyclerView.ItemAnimator.ItemHolderInfo preInfo = new RecyclerView.ItemAnimator.ItemHolderInfo();
        preInfo.left = (int) viewHolder.itemView.getTranslationX();
        preInfo.top = (int) viewHolder.itemView.getTranslationY();

        // Setup postInfo with all values at 0 (the default). The intent is to settle in the final position.
        RecyclerView.ItemAnimator.ItemHolderInfo postInfo = new RecyclerView.ItemAnimator.ItemHolderInfo();

        // Clear current translation values to prevent them from being added on top of preInfo.
        setTranslation(viewHolder, 0f, 0f);

        // Animate the move, stopping internally when done.
        if (itemAnimator != null && itemAnimator.animatePersistence(viewHolder, preInfo, postInfo)) {
            itemAnimator.runPendingAnimations();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        return handleMotionEvent(e);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        handleMotionEvent(e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            stop();
        }
    }

    private boolean handleMotionEvent(@NonNull MotionEvent event) {
        if (mState != STATE_NONE && mState != STATE_STOPPING) {
            int action = event.getActionMasked();
            int x = (int) event.getX();
            int y = (int) event.getY();

            if (mState == STATE_STARTING && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
                // Start the drag.
                startInternal();
                mTouchStartX = mTouchCurrentX = mTouchPivotX = x;
                mTouchStartY = mTouchCurrentY = mTouchPivotY = y;
                return true;
            }
            if (mState != STATE_RECOVERING && action == MotionEvent.ACTION_MOVE) {
                // Update the drag, the touch event is moving.
                moveInternal(x, y);
                mTouchCurrentX = x;
                mTouchCurrentY = y;
                if (Math.abs(mTouchCurrentX - mTouchPivotX) > mTouchSlop) {
                    mTouchDirectionX = Math.signum(mTouchCurrentX - mTouchPivotX);
                    mTouchPivotX = mTouchCurrentX;
                }
                if (Math.abs(mTouchCurrentY - mTouchPivotY) > mTouchSlop) {
                    mTouchDirectionY = Math.signum(mTouchCurrentY - mTouchPivotY);
                    mTouchPivotY = mTouchCurrentY;
                }
                mRecyclerView.invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                // Stop the drag, the touch event has ended.
                stop();
                return true;
            }
        }
        return false;
    }

    @Override
    public void getItemOffsets(
            Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.setEmpty();
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (mState != STATE_NONE && mState != STATE_STOPPING) {
            RecyclerView.ViewHolder viewHolder = mTracker.getViewHolder();

            if (mState == STATE_TRACKING || mState == STATE_DRAGGING) {
                // Update location.
                updateLocation(viewHolder);
            }

            if (viewHolder != null) {
                if (mState == STATE_DRAGGING) {
                    // Offset dragged item.
                    setTranslation(
                            viewHolder,
                            mLocation.left - viewHolder.itemView.getLeft(),
                            mLocation.top - viewHolder.itemView.getTop());

                    // Swap with adjacent views if necessary.
                    handleSwap(viewHolder);
                }

                // Handle scrolling when on the edges.
                handleScroll(viewHolder, mState != STATE_DRAGGING);
            }
        }
    }

    private void setTranslation(@NonNull RecyclerView.ViewHolder viewHolder, float translationX, float translationY) {
        viewHolder.itemView.setTranslationX(translationX);
        viewHolder.itemView.setTranslationY(translationY);
    }

    /**
     * Updates the location using the touch position relative to start.
     */
    private void updateLocation(@Nullable RecyclerView.ViewHolder viewHolder) {
        int left = mStartLeft + (mTouchCurrentX - mTouchStartX);
        int top = mStartTop + (mTouchCurrentY - mTouchStartY);

        if (viewHolder != null) {
            int width = viewHolder.itemView.getWidth();
            int height = viewHolder.itemView.getHeight();
            int minLeft = mRecyclerView.getPaddingLeft();
            int maxLeft = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight() - width;
            int minTop = mRecyclerView.getPaddingTop();
            int maxTop = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom() - height;

            left = MathUtils.clamp(left, minLeft, maxLeft);
            top = MathUtils.clamp(top, minTop, maxTop);
        }

        mLocation.offsetTo(left, top);
    }

    /**
     * Scrolls the {@link RecyclerView} when close to the edges and swapping views. Scrolling starts when the
     * {@link androidx.recyclerview.widget.RecyclerView.ViewHolder} is width / height away from the edge,
     * and accelerates from there.
     *
     * @param decelerate if {@code true}, the scroll is decelerated regardless of the proximity to the edges.
     */
    private void handleScroll(@NonNull RecyclerView.ViewHolder viewHolder, boolean decelerate) {
        int scrollByX = 0;
        int scrollByY = 0;
        int direction;

        if (mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            direction = mLocation.centerX() < mRecyclerView.getWidth() / 2f ? -1 : 1;
            if (!decelerate && canScrollHorizontally(direction)
                    && Math.abs(viewHolder.itemView.getTranslationX()) < mRecyclerView.getWidth() / 2f) {
                if (direction == -1) {
                    int left = mRecyclerView.getPaddingLeft();
                    float boundaryLeft = left + mLocation.width();
                    if (mLocation.left <= boundaryLeft) {
                        updateScrollSpeedAccelerating(1 - ((mLocation.left - left) / (boundaryLeft - left)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                } else {
                    int right = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight();
                    float boundaryRight = right - mLocation.width();
                    if (mLocation.right >= boundaryRight) {
                        updateScrollSpeedAccelerating(1 - ((right - mLocation.right) / (right - boundaryRight)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                }
            } else {
                updateScrollSpeedDecelerating();
            }
            scrollByX = (int) mScrollSpeed;
        } else {
            direction = mLocation.centerY() < mRecyclerView.getHeight() / 2f ? -1 : 1;
            if (!decelerate && canScrollVertically(direction)
                    && Math.abs(viewHolder.itemView.getTranslationY()) < mRecyclerView.getHeight() / 2f) {
                if (direction == -1) {
                    int top = mRecyclerView.getPaddingTop();
                    float boundaryTop = top + mLocation.height();
                    if (mLocation.top <= boundaryTop) {
                        updateScrollSpeedAccelerating(1 - ((mLocation.top - top) / (boundaryTop - top)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                } else {
                    int bottom = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom();
                    float boundaryBottom = bottom - mLocation.height();
                    if (mLocation.bottom >= boundaryBottom) {
                        updateScrollSpeedAccelerating(1 - ((bottom - mLocation.bottom) / (bottom - boundaryBottom)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                }
            } else {
                updateScrollSpeedDecelerating();
            }
            scrollByY = (int) mScrollSpeed;
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
                holder = mRecyclerView.findViewHolderForAdapterPosition(0);
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
                holder = mRecyclerView.findViewHolderForAdapterPosition(getItemCount() - 1);
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
     * Alternative to {@link RecyclerView#canScrollHorizontally(int)} that is considerably faster and more suited to
     * use while drawing. Doesn't compute scroll offsets, ranges or extents, and just relies on the child views.
     */
    private boolean canScrollHorizontally(int direction) {
        int childCount = mRecyclerView.getChildCount();
        if (childCount > 0) {
            RecyclerView.ViewHolder holder;
            if (direction < 0) {
                holder = mRecyclerView.findViewHolderForAdapterPosition(0);
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
                holder = mRecyclerView.findViewHolderForAdapterPosition(getItemCount() - 1);
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
     * Updates the scroll speed for an accelerating motion, up to {@code mScrollSpeedMax}.
     */
    private void updateScrollSpeedAccelerating(float fraction) {
        mScrollSpeed = Math.min(mScrollSpeed * 1.04f, mScrollSpeedMax * fraction);
        mScrollSpeed = Math.max(mScrollSpeed, 1f);
    }

    /**
     * Updates the scroll speed for a decelerating motion, until {@code 0} is reached.
     */
    private void updateScrollSpeedDecelerating() {
        mScrollSpeed = mScrollSpeed > 1f ? mScrollSpeed / 1.18f : 0f;
    }

    /**
     * Swaps {@code viewHolder} whenever it overlaps the boundaries of another {@link RecyclerView.ViewHolder}.
     */
    private void handleSwap(@NonNull RecyclerView.ViewHolder viewHolder) {
        RecyclerView.ViewHolder target = null;
        int position = mTracker.getPosition();
        if (mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            if (mTouchDirectionX < 0) {
                RecyclerView.ViewHolder left = mRecyclerView.findViewHolderForAdapterPosition(position - 1);
                if (left != null && left.itemView.getLeft() >= mLocation.left) {
                    target = left;
                }
            } else if (mTouchDirectionX > 0) {
                RecyclerView.ViewHolder right = mRecyclerView.findViewHolderForAdapterPosition(position + 1);
                if (right != null && right.itemView.getRight() <= mLocation.right) {
                    target = right;
                }
            }
        } else {
            if (mTouchDirectionY < 0) {
                RecyclerView.ViewHolder top = mRecyclerView.findViewHolderForAdapterPosition(position - 1);
                if (top != null && top.itemView.getTop() >= mLocation.top) {
                    target = top;
                }
            } else if (mTouchDirectionY > 0) {
                RecyclerView.ViewHolder bottom = mRecyclerView.findViewHolderForAdapterPosition(position + 1);
                if (bottom != null && bottom.itemView.getBottom() <= mLocation.bottom) {
                    target = bottom;
                }
            }
        }
        int targetPosition;
        if (target != null && (targetPosition = target.getAdapterPosition()) != RecyclerView.NO_POSITION) {
            int newPosition = mCallback.onDragTo(viewHolder, targetPosition);
            if (newPosition != position) {
                // Prevent unintended scrolling when swapping the very first and last views,
                // when they act as anchor views. Not doing so triggers unintended scrolling.
                if (mLayoutManager.getReverseLayout()) {
                    int lastPosition = getItemCount() - 1;
                    if (position == lastPosition || newPosition == lastPosition) {
                        mLayoutManager.scrollToPosition(lastPosition);
                    }
                } else if (position == 0 || newPosition == 0) {
                    mLayoutManager.scrollToPosition(0);
                }
            }
        }
    }

    @Override
    public int onGetChildDrawingOrder(int childCount, int i) {
        RecyclerView.ViewHolder viewHolder = mTracker.getViewHolder();
        if (viewHolder == null) {
            return i;
        }
        int itemPosition = mRecyclerView.indexOfChild(viewHolder.itemView);
        if (itemPosition == -1) {
            // RV will on rare occasions return a VH that has an adapter position, but its item view is not present
            // in the hierarchy. This is an intermediate state which we can safely ignore.
            return i;
        }
        if (i == childCount - 1) {
            return itemPosition;
        } else {
            return i < itemPosition ? i : i + 1;
        }
    }

    private int getItemCount() {
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter != null) {
            return adapter.getItemCount();
        } else {
            return 0;
        }
    }

    /**
     * Keeps track of the drag & drop position and holder across swaps, scrolls and adapter changes.
     */
    private class Tracker extends RecyclerView.AdapterDataObserver {
        private int position;
        private RecyclerView.ViewHolder viewHolder;

        Tracker(int position) {
            this.position = position;
        }

        /**
         * @return the position being dragged, updated automatically through adapter changes.
         */
        int getPosition() {
            return position;
        }

        /**
         * @return the view holder being dragged, updated automatically through adapter changes, swaps, scrolls, etc.
         */
        RecyclerView.ViewHolder getViewHolder() {
            if (viewHolder != null && viewHolder.itemView.getParent() != mRecyclerView) {
                teardownViewHolder(false);
            }
            if (viewHolder == null) {
                setupViewHolder(false);
            }
            if (viewHolder == null && !mRecyclerView.isLayoutRequested()) {
                mRecyclerView.scrollToPosition(position);
            }
            return viewHolder;
        }

        RecyclerView.ViewHolder createViewHolder() {
            return setupViewHolder(true);
        }

        void destroyViewHolder() {
            teardownViewHolder(true);
        }

        private RecyclerView.ViewHolder setupViewHolder(boolean create) {
            viewHolder = mRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                mCallback.onDragStarted(viewHolder, create);
            }
            return viewHolder;
        }

        private void teardownViewHolder(boolean destroy) {
            setTranslation(viewHolder, 0f, 0f);
            mCallback.onDragStopped(viewHolder, destroy);
            viewHolder = null;
        }

        @Override
        public void onChanged() {
            stop(true);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (positionStart < position) {
                position += itemCount;
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (position >= positionStart && position < positionStart + itemCount) {
                stop(true);
            } else if (positionStart < position) {
                position -= itemCount;
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (position >= fromPosition && position < fromPosition + itemCount) {
                position += toPosition - fromPosition;
            } else if (fromPosition < toPosition && position >= fromPosition + itemCount && position <= toPosition) {
                position -= itemCount;
            } else if (fromPosition > toPosition && position >= toPosition && position <= fromPosition) {
                position += itemCount;
            }
        }
    }

    public interface Callback {
        /**
         * A drag has started or its {@link RecyclerView.ViewHolder} has changed. {@code holder} can be setup in this
         * callback (e.g. elevate, change background, etc).
         *
         * This can be called multiple times during a single drag. {@code create} will be true when the drag is
         * starting, and false for all subsequent calls following {@link RecyclerView.ViewHolder} changes.
         *
         * @param create true when the drag has just started, false when its being resumed
         *               (e.g. after scroll or re-entering bounds).
         */
        void onDragStarted(@NonNull RecyclerView.ViewHolder holder, boolean create);

        /**
         * A drag has moved. The callback determines whether the drag should visually continue on screen
         * (return {@code true}), simply be tracked (return {@code false}), or stopped (call {@link #stop()}).
         *
         * Most implementations should simply return {@code true}. This becomes useful when implementations want to
         * trigger certain behaviors or workflows based off of dragging coordinates. Examples include indenting while
         * reordering, handing off control to Android's native drag & drop when dragging out of bounds, etc.
         *
         * @param x x-coordinate of the drag, in the parent's coordinates.
         * @param y y-coordinate of the drag, in the parent's coordinates.
         * @return {@code true} for the {@code holder} to drag on screen, {@code false} to track without visual changes.
         */
        boolean onDragMoved(@NonNull RecyclerView.ViewHolder holder, int x, int y);

        /**
         * Move {@code holder} to {@code adapterPosition} and notify the {@link RecyclerView.Adapter}.
         *
         * Implementations can move {@code holder} to any position. {@code target} is a mere indication of where
         * the user is trying to move to, which implementations should typically honor, but not mandatorily.
         *
         * @return the adapter position that {@code holder} moved to.
         */
        int onDragTo(@NonNull RecyclerView.ViewHolder holder, int to);

        /**
         * A drag has stopped or its {@link RecyclerView.ViewHolder} will change. Any setup done to {@code holder} in
         * {@link #onDragStarted(RecyclerView.ViewHolder, boolean)} should be undone here.
         *
         * This can be called multiple times during a single drag. {@code destroy} will be true when the drag is
         * ending, and false for any prior calls following {@link RecyclerView.ViewHolder} changes.
         *
         * @param destroy true when the drag is ending, false when it's pausing
         *                (e.g. scrolling or exiting bounds).
         */
        void onDragStopped(@NonNull RecyclerView.ViewHolder holder, boolean destroy);
    }
}
