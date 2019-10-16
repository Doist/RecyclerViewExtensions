package io.doist.recyclerviewext.dragdrop;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class for adding drag & drop to {@link RecyclerView}.
 *
 * It works with {@link RecyclerView}, {@link LinearLayoutManager} and a {@link Callback}.
 *
 * Call {@link #start(RecyclerView.ViewHolder)} during or right before a motion event to start dragging that specific
 * {@link RecyclerView.ViewHolder}. Call {@link #stop()} to stop the ongoing drag at the current position.
 */
public class DragDropHelper extends RecyclerView.ItemDecoration
        implements RecyclerView.OnItemTouchListener, RecyclerView.ChildDrawingOrderCallback,
                   RecyclerView.OnChildAttachStateChangeListener {
    public static final String LOG_TAG = DragDropHelper.class.getSimpleName();

    private static final float SCROLL_SPEED_MAX_DP = 12;

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

    private static final Interpolator SCROLL_INTERPOLATOR = new AccelerateInterpolator();

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private Callback mCallback;

    /**
     * {@link RecyclerView.ViewHolder} currently being dragged.
     */
    private RecyclerView.ViewHolder mViewHolder;

    /**
     * Current state. Can be {@link #STATE_NONE}, {@link #STATE_STARTING}, {@link #STATE_TRACKING},
     * {@link #STATE_DRAGGING} or {@link #STATE_RECOVERING}. For any of these except the first,
     * {@link #mViewHolder} is not null.
     */
    private int mState = STATE_NONE;

    /**
     * Dragged item's location on screen.
     */
    private final Rect mLocation = new Rect();
    private int mStartLeft;
    private int mStartTop;

    private int mTouchStartX;
    private int mTouchStartY;
    private int mTouchCurrentX;
    private int mTouchCurrentY;

    private float mScrollSpeed = 0f;
    private float mScrollSpeedMax;

    /**
     * Attaches {@link DragDropHelper} to {@code recyclerView}. If already attached to a {@link RecyclerView}, it
     * detaches from the previous one. If {@code null} is provided, it detaches from the current {@link RecyclerView}.
     *
     * {@link DragDropHelper} uses {@link RecyclerView.ItemDecoration}, {@link RecyclerView.OnItemTouchListener} and
     * {@link RecyclerView.OnChildAttachStateChangeListener} internally, which are set or removed here.
     */
    public void attach(@Nullable RecyclerView recyclerView, @NonNull Callback callback) {
        if (mRecyclerView != recyclerView) {
            if (mRecyclerView != null) {
                mRecyclerView.removeItemDecoration(this);
                mRecyclerView.removeOnItemTouchListener(this);
                mRecyclerView.removeOnChildAttachStateChangeListener(this);
            }
            mRecyclerView = recyclerView;
            if (mRecyclerView != null) {
                mScrollSpeedMax = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, SCROLL_SPEED_MAX_DP,
                        mRecyclerView.getResources().getDisplayMetrics());
                mRecyclerView.addItemDecoration(this, 0);
                mRecyclerView.addOnItemTouchListener(this);
                mRecyclerView.addOnChildAttachStateChangeListener(this);
            }
        }
        mCallback = callback;
    }

    /**
     * Starts drag and drop for {@code holder} as soon as there is a touch event, or immediately if one is in progress.
     *
     * @return {@code true} if drag is starting, {@code false} if not.
     */
    public boolean start(@NonNull RecyclerView.ViewHolder holder) {
        // Ensure current state is valid and that no other drag is currently in progress.
        if (holder == mViewHolder) {
            Log.w(LOG_TAG, "View holder is already being dragged");
            return false;
        }
        if (holder.itemView.getParent() != mRecyclerView) {
            Log.w(LOG_TAG, "View holder which is not a child of this RecyclerView");
            return false;
        }
        if (holder.getAdapterPosition() == RecyclerView.NO_POSITION) {
            Log.w(LOG_TAG, "View holder doesn't have an adapter position");
            return false;
        }
        if (mState == STATE_STOPPING) {
            Log.w(LOG_TAG, "A drag is currently being stopped");
            return false;
        } else if (mState != STATE_NONE) {
            // Stop current drag immediately before starting new one.
            stop(true);
        }

        // Grab reference to LinearLayoutManager.
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            Log.w(LOG_TAG, "RecyclerView is not using LinearLayoutManager");
            return false;
        }
        mLayoutManager = (LinearLayoutManager) layoutManager;

        // Grab holder and update state.
        mViewHolder = holder;
        mState = STATE_STARTING;

        // Get the initial location.
        mStartLeft = mViewHolder.itemView.getLeft();
        mStartTop = mViewHolder.itemView.getTop();
        mLocation.set(mStartLeft, mStartTop, mViewHolder.itemView.getRight(), mViewHolder.itemView.getBottom());

        // Notify callback that the drag has started.
        mCallback.onDragStarted(mViewHolder, true);

        // Drag will start.
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
        int state = mCallback.onDragMoved(mViewHolder, x, y) ? STATE_DRAGGING : STATE_TRACKING;
        if (mState == STATE_DRAGGING && state == STATE_TRACKING) {
            recoverInternal(mRecyclerView.getItemAnimator());
            mCallback.onDragStopped(mViewHolder, false);
        } else if (mState == STATE_TRACKING && state == STATE_DRAGGING) {
            mCallback.onDragStarted(mViewHolder, false);
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
            if (!now && mState == STATE_DRAGGING && itemAnimator != null) {
                recoverInternal(itemAnimator);

                // Stop internally after animations are done.
                itemAnimator.isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        stopInternal();
                    }
                });
            } else {
                if (mState == STATE_RECOVERING && itemAnimator != null) {
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

        // Remove child order drawing callback.
        mRecyclerView.setChildDrawingOrderCallback(null);

        // Reset the scroll speed.
        mScrollSpeed = 0;

        // Teardown holder and clear it.
        setTranslation(0f, 0f);
        mCallback.onDragStopped(mViewHolder, true);
        mRecyclerView.invalidateItemDecorations();
        mState = STATE_NONE;
        mViewHolder = null;
    }

    private void recoverInternal(@Nullable RecyclerView.ItemAnimator itemAnimator) {
        mState = STATE_RECOVERING;

        // Setup preInfo with current translation values.
        RecyclerView.ItemAnimator.ItemHolderInfo preInfo = new RecyclerView.ItemAnimator.ItemHolderInfo();
        preInfo.left = (int) mViewHolder.itemView.getTranslationX();
        preInfo.top = (int) mViewHolder.itemView.getTranslationY();

        // Setup postInfo with all values at 0 (the default). The intent is to settle in the final position.
        RecyclerView.ItemAnimator.ItemHolderInfo postInfo = new RecyclerView.ItemAnimator.ItemHolderInfo();

        // Clear current translation values to prevent them from being added on top of preInfo.
        setTranslation(0f, 0f);

        // Animate the move, stopping internally when done.
        if (itemAnimator != null && itemAnimator.animatePersistence(mViewHolder, preInfo, postInfo)) {
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

    private boolean handleMotionEvent(MotionEvent event) {
        if (mState != STATE_NONE && mState != STATE_STOPPING) {
            int action = event.getActionMasked();
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (mState == STATE_STARTING && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
                // Start the drag.
                startInternal();
                mTouchStartX = mTouchCurrentX = x;
                mTouchStartY = mTouchCurrentY = y;
                return true;
            }
            if (mState != STATE_RECOVERING && action == MotionEvent.ACTION_MOVE) {
                moveInternal(x, y);
                // Update the drag, the touch event is moving.
                mTouchCurrentX = x;
                mTouchCurrentY = y;
                mRecyclerView.invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                // Stop the drag, the touch event as ended.
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
            if (mState == STATE_TRACKING || mState == STATE_DRAGGING) {
                // Update location.
                updateLocation();
            }

            if (mState == STATE_DRAGGING) {
                // Offset dragged item.
                setTranslation(
                        mLocation.left - mViewHolder.itemView.getLeft(),
                        mLocation.top - mViewHolder.itemView.getTop());

                // Swap with adjacent views if necessary.
                handleSwap();
            }

            // Handle scrolling when on the edges.
            handleScroll(mState == STATE_DRAGGING);
        }
    }

    private void setTranslation(float translationX, float translationY) {
        mViewHolder.itemView.setTranslationX(translationX);
        mViewHolder.itemView.setTranslationY(translationY);
    }

    /**
     * Updates the location using the touch position relative to start and the boundaries imposed by
     * {@link Callback#canSwap(RecyclerView.ViewHolder, RecyclerView.ViewHolder)}.
     */
    private void updateLocation() {
        int position = mViewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            int width = mViewHolder.itemView.getWidth();
            int height = mViewHolder.itemView.getHeight();
            int minLeft = mRecyclerView.getPaddingLeft();
            int maxLeft = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight() - width;
            int minTop = mRecyclerView.getPaddingTop();
            int maxTop = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom() - height;

            RecyclerView.ViewHolder holderAbove =
                    mRecyclerView.findViewHolderForAdapterPosition(position - 1);
            if (holderAbove != null && !mCallback.canSwap(mViewHolder, holderAbove)) {
                if (mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
                    minLeft = Math.max(minLeft, holderAbove.itemView.getRight());
                } else {
                    minTop = Math.max(minTop, holderAbove.itemView.getBottom());
                }
            }

            RecyclerView.ViewHolder holderBelow =
                    mRecyclerView.findViewHolderForAdapterPosition(position + 1);
            if (holderBelow != null && !mCallback.canSwap(mViewHolder, holderBelow)) {
                if (mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
                    maxLeft = Math.min(maxLeft, holderBelow.itemView.getLeft() - width);
                } else {
                    maxTop = Math.min(maxTop, holderBelow.itemView.getTop() - height);
                }
            }

            int left = Math.max(minLeft, Math.min(maxLeft, mStartLeft + (mTouchCurrentX - mTouchStartX)));
            int top = Math.max(minTop, Math.min(maxTop, mStartTop + (mTouchCurrentY - mTouchStartY)));
            mLocation.offsetTo(left, top);
        }
    }

    /**
     * Scrolls the {@link RecyclerView} when close to the edges. Scrolling starts when {@link #mViewHolder} is
     * width / height away from the edge and accelerates from there.
     *
     * @param decelerate if {@code true}, the scroll is decelerated regardless of the proximity to the edges.
     */
    private void handleScroll(boolean decelerate) {
        int scrollByX = 0;
        int scrollByY = 0;
        int direction;

        if (mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
            float itemCenterX = (mLocation.left + mLocation.right) / 2f;
            direction = itemCenterX < mRecyclerView.getWidth() / 2f ? -1 : 1;
            if (canScrollHorizontally(direction)) {
                float itemWidth = mLocation.right - mLocation.left;
                if (direction == -1) {
                    int left = mRecyclerView.getPaddingLeft();
                    float boundaryLeft = left + itemWidth;
                    if (!decelerate && mLocation.left <= boundaryLeft) {
                        updateScrollSpeedAccelerating(1 - ((mLocation.left - left) / (boundaryLeft - left)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                } else {
                    int right = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight();
                    float boundaryRight = right - itemWidth;
                    if (!decelerate && mLocation.right >= boundaryRight) {
                        updateScrollSpeedAccelerating(1 - ((right - mLocation.right) / (right - boundaryRight)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                }
            } else if (mScrollSpeed > 0) {
                // Slow down scroll to show the edge glow.
                updateScrollSpeedDecelerating();
            }
            scrollByX = (int) mScrollSpeed;
        } else {
            float itemCenterY = (mLocation.top + mLocation.bottom) / 2f;
            direction = itemCenterY < mRecyclerView.getHeight() / 2f ? -1 : 1;
            if (canScrollVertically(direction)) {
                float itemHeight = mLocation.bottom - mLocation.top;
                if (direction == -1) {
                    int top = mRecyclerView.getPaddingTop();
                    float boundaryTop = top + itemHeight;
                    if (!decelerate && mLocation.top <= boundaryTop) {
                        updateScrollSpeedAccelerating(1 - ((mLocation.top - top) / (boundaryTop - top)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                } else {
                    int bottom = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom();
                    float boundaryBottom = bottom - itemHeight;
                    if (!decelerate && mLocation.bottom >= boundaryBottom) {
                        updateScrollSpeedAccelerating(1 - ((bottom - mLocation.bottom) / (bottom - boundaryBottom)));
                    } else {
                        updateScrollSpeedDecelerating();
                    }
                }
            } else if (mScrollSpeed > 0) {
                // Slow down scroll to show the edge glow.
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
     *
     * @param fraction How much of the calculated scroll speed should be applied, depending on how close to the
     *                 boundary it is. [0..1].
     */
    private void updateScrollSpeedAccelerating(float fraction) {
        mScrollSpeed = Math.min(mScrollSpeedMax * SCROLL_INTERPOLATOR.getInterpolation(fraction), mScrollSpeed * 1.06f);
        if (mScrollSpeed < 1) {
            mScrollSpeed = 1;
        }
    }

    /**
     * Updates the scroll speed for a decelerating motion, until {@code 0} is reached.
     */
    private void updateScrollSpeedDecelerating() {
        mScrollSpeed = mScrollSpeedMax > 1 ? mScrollSpeed / 1.2f : 0f;
    }

    /**
     * Swaps {@link #mViewHolder} whenever it overlaps the boundaries of another {@link RecyclerView.ViewHolder}.
     */
    private void handleSwap() {
        int position = mViewHolder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            RecyclerView.ViewHolder target = null;
            if (mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
                RecyclerView.ViewHolder left = mRecyclerView.findViewHolderForAdapterPosition(position - 1);
                if (left != null && left.itemView.getLeft() >= mLocation.left) {
                    target = left;
                } else {
                    RecyclerView.ViewHolder right = mRecyclerView.findViewHolderForAdapterPosition(position + 1);
                    if (right != null && right.itemView.getRight() <= mLocation.right) {
                        target = right;
                    }
                }
            } else {
                RecyclerView.ViewHolder top = mRecyclerView.findViewHolderForAdapterPosition(position - 1);
                if (top != null && top.itemView.getTop() >= mLocation.top) {
                    target = top;
                } else {
                    RecyclerView.ViewHolder bottom = mRecyclerView.findViewHolderForAdapterPosition(position + 1);
                    if (bottom != null && bottom.itemView.getBottom() <= mLocation.bottom) {
                        target = bottom;
                    }
                }
            }

            int targetPosition;
            if (target != null && (targetPosition = target.getAdapterPosition()) != RecyclerView.NO_POSITION) {
                // Prevent unintended scrolling when swapping the very first and last views, as they are used as anchor
                // views hence hinting RV to scroll while animating.
                if (position == 0 || targetPosition == 0) {
                    mLayoutManager.scrollToPosition(0);
                } else {
                    int lastPosition = getItemCount() - 1;
                    if (position == lastPosition || targetPosition == lastPosition) {
                        mLayoutManager.scrollToPosition(lastPosition);
                    }
                }

                mCallback.onSwap(mViewHolder, target);
            }
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

    @Override
    public int onGetChildDrawingOrder(int childCount, int i) {
        int itemPosition = mRecyclerView.indexOfChild(mViewHolder.itemView);
        if (i == childCount - 1) {
            return itemPosition;
        } else {
            return i < itemPosition ? i : i + 1;
        }
    }

    @Override
    public void onChildViewAttachedToWindow(@NonNull View view) {
    }

    @Override
    public void onChildViewDetachedFromWindow(@NonNull View view) {
        RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
        if (holder == null) {
            return;
        }
        if (holder == mViewHolder) {
            stop(true);
        }
    }

    public interface Callback {
        /**
         * A drag has started for {@code holder}. It can be the very start of the drag, or a resume as
         * signalled by the result of {@link #onDragMoved(RecyclerView.ViewHolder, int, int)}.
         *
         * @param create true when the drag has just started, false when its being resumed (eg. re-entered bounds).
         */
        void onDragStarted(@NonNull RecyclerView.ViewHolder holder, boolean create);

        /**
         * A drag has moved. The callback determines whether the drag should continue on screen (return true), simply
         * be tracked to be resumed later (return false), or stopped (call {@link #stop()}).
         *
         * @param x x-coordinate of the drag, in the parent's coordinates.
         * @param y y-coordinate of the drag, in the parent's coordinates.
         * @return true to have the {@code holder} track the drag on screen.
         */
        boolean onDragMoved(@NonNull RecyclerView.ViewHolder holder, int x, int y);

        /**
         * Called to determine whether {@code holder} can be swapped with {@code target}.
         * {@link #onSwap(RecyclerView.ViewHolder, RecyclerView.ViewHolder)} might be called later on
         * if true.
         */
        boolean canSwap(@NonNull RecyclerView.ViewHolder holder, @NonNull RecyclerView.ViewHolder target);

        /**
         * Swap {@code holder} to {@code target}'s adapter position and notify the {@link RecyclerView.Adapter}.
         */
        void onSwap(@NonNull RecyclerView.ViewHolder holder, @NonNull RecyclerView.ViewHolder target);

        /**
         * A drag has stopped for {@code holder}. It can be the final ending of the drag, or a pause as
         * signalled by the result of {@link #onDragMoved(RecyclerView.ViewHolder, int, int)}.
         *
         * @param destroy true when the drag is ending, false when it's pausing (eg. exited bounds).
         */
        void onDragStopped(@NonNull RecyclerView.ViewHolder holder, boolean destroy);
    }
}
