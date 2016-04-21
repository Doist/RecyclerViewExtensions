package io.doist.recyclerviewext.dragdrop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

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

    private static final int STATE_NONE = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_RECOVERING = 3;

    private static final Interpolator SCROLL_INTERPOLATOR = new AccelerateInterpolator();

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private Callback mCallback;

    /**
     * {@link RecyclerView.ViewHolder} currently being dragged.
     */
    private RecyclerView.ViewHolder mViewHolder;

    /**
     * Current state. Can be {@link #STATE_NONE}, {@link #STATE_STARTING}, {@link #STATE_DRAGGING} or
     * {@link #STATE_RECOVERING}. For any of these except the first, {@link #mViewHolder} is not null.
     */
    private int mState = STATE_NONE;

    /**
     * Dragged item's location on screen.
     */
    private Rect mLocation = new Rect();
    private int mStartLeft;
    private int mStartTop;

    private int mTouchStartX;
    private int mTouchStartY;
    private int mTouchCurrentX;
    private int mTouchCurrentY;

    private float mScrollSpeed = 0f;
    private float mScrollSpeedMax;

    public DragDropHelper(@NonNull Callback callback) {
        mCallback = callback;
    }

    /**
     * Attaches {@link DragDropHelper} to {@code recyclerView}. If already attached to a {@link RecyclerView}, it first
     * detaches from the previous one. If {@code null} is provided, it detaches from the current {@link RecyclerView}.
     *
     * {@link DragDropHelper} uses {@link RecyclerView.ItemDecoration}, {@link RecyclerView.OnItemTouchListener} and
     * {@link RecyclerView.OnChildAttachStateChangeListener} internally, which are set or removed here.
     */
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return;
        }
        if (mRecyclerView != null) {
            mRecyclerView.removeItemDecoration(this);
            mRecyclerView.removeOnItemTouchListener(this);
            mRecyclerView.removeOnChildAttachStateChangeListener(this);
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView != null) {
            mScrollSpeedMax = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SCROLL_SPEED_MAX_DP,
                                                        mRecyclerView.getResources().getDisplayMetrics());
            mRecyclerView.addItemDecoration(this, 0);
            mRecyclerView.addOnItemTouchListener(this);
            mRecyclerView.addOnChildAttachStateChangeListener(this);
        }
    }

    /**
     * Starts drag and drop for {@code holder} as soon as there is a touch even, or immediately if one is in progress.
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
        if (mState > STATE_NONE) {
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

        // Notify callback that the drag has started.
        mCallback.onDragStarted(mViewHolder);

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

        // Get the initial location.
        int left = mViewHolder.itemView.getLeft();
        int top = mViewHolder.itemView.getTop();
        mLocation.set(left, top, mViewHolder.itemView.getRight(), mViewHolder.itemView.getBottom());
        mStartLeft = left;
        mStartTop = top;

        // Update state.
        mState = STATE_DRAGGING;
    }

    /**
     * Stops the current drag and drop, animating the item into its final position.
     */
    public void stop() {
        stop(false);
    }

    private void stop(boolean now) {
        if (mState > STATE_NONE) {
            if (!now && mState == STATE_DRAGGING) {
                mState = STATE_RECOVERING;
                RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
                long duration = itemAnimator != null ? itemAnimator.getMoveDuration() : 200L;
                mViewHolder.setIsRecyclable(false);
                mViewHolder.itemView.animate().translationX(0).translationY(0).setDuration(duration).setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mViewHolder.setIsRecyclable(true);
                                stopInternal();
                            }
                        });
            } else {
                if (mState == STATE_RECOVERING) {
                    mViewHolder.itemView.animate().cancel();
                } else {
                    stopInternal();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void stopInternal() {
        // Remove child order drawing callback.
        mRecyclerView.setChildDrawingOrderCallback(null);

        // Reset the scroll speed.
        mScrollSpeed = 0;

        // Teardown holder and clear it.
        mViewHolder.itemView.setTranslationX(0f);
        mViewHolder.itemView.setTranslationY(0f);
        mCallback.onDragStopped(mViewHolder);
        mRecyclerView.invalidate();
        mState = STATE_NONE;
        mViewHolder = null;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return handleMotionEvent(e);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        handleMotionEvent(e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            stop();
        }
    }

    private boolean handleMotionEvent(MotionEvent event) {
        if (mState > STATE_NONE) {
            int action = MotionEventCompat.getActionMasked(event);
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (mState == STATE_STARTING && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
                // Start the drag.
                startInternal();
                mTouchStartX = mTouchCurrentX = x;
                mTouchStartY = mTouchCurrentY = y;
                return true;
            }
            if (mState < STATE_RECOVERING && action == MotionEvent.ACTION_MOVE) {
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
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.setEmpty();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);

        if (mState > STATE_NONE) {
            boolean dragging = mState == STATE_DRAGGING;

            if (dragging) {
                // Update location.
                updateLocation();

                // Offset dragged item.
                mViewHolder.itemView.setTranslationX(mLocation.left - mViewHolder.itemView.getLeft());
                mViewHolder.itemView.setTranslationY(mLocation.top - mViewHolder.itemView.getTop());

                if (mViewHolder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    // Swap with adjacent views if necessary.
                    handleSwap();
                }
            }

            // Handle scrolling when on the edges.
            handleScroll(!dragging);
        }
    }

    /**
     * Updates the location using the touch position relative to start and the boundaries imposed by
     * {@link Callback#canSwap(RecyclerView.ViewHolder, RecyclerView.ViewHolder)}.
     */
    private void updateLocation() {
        int width = mViewHolder.itemView.getWidth();
        int height = mViewHolder.itemView.getHeight();
        int minLeft = mRecyclerView.getPaddingLeft();
        int maxLeft = mRecyclerView.getWidth() - mRecyclerView.getPaddingRight() - width;
        int minTop = mRecyclerView.getPaddingTop();
        int maxTop = mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom() - height;

        RecyclerView.ViewHolder holderAbove =
                mRecyclerView.findViewHolderForAdapterPosition(mViewHolder.getAdapterPosition() - 1);
        if (holderAbove != null && !mCallback.canSwap(mViewHolder, holderAbove)) {
            if (mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL) {
                minLeft = Math.max(minLeft, holderAbove.itemView.getRight());
            } else {
                minTop = Math.max(minTop, holderAbove.itemView.getBottom());
            }
        }

        RecyclerView.ViewHolder holderBelow =
                mRecyclerView.findViewHolderForAdapterPosition(mViewHolder.getAdapterPosition() + 1);
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
                scrollByX = (int) mScrollSpeed;
            } else if (mScrollSpeed > 0) {
                scrollByX = (int) mScrollSpeed; // One last scroll to show edge glow.
                mScrollSpeed = 0;
            }
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
                holder = mRecyclerView.findViewHolderForAdapterPosition(mRecyclerView.getAdapter().getItemCount() - 1);
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
                holder = mRecyclerView.findViewHolderForAdapterPosition(mRecyclerView.getAdapter().getItemCount() - 1);
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
        if (position != -1) {
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

            if (target != null && target.getAdapterPosition() != -1) {
                // Prevent unintended scrolling when swapping the very first and last views, as they are used as anchor
                // views hence hinting RV to scroll while animating.
                int holderPosition = mViewHolder.getAdapterPosition();
                int targetPosition = target.getAdapterPosition();
                if (holderPosition == 0 || targetPosition == 0) {
                    mLayoutManager.scrollToPosition(0);
                } else {
                    int lastPosition = mRecyclerView.getAdapter().getItemCount() - 1;
                    if(holderPosition == lastPosition || targetPosition == lastPosition) {
                        mLayoutManager.scrollToPosition(lastPosition);
                    }
                }

                mCallback.onSwap(mViewHolder, target);
            }
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
    public void onChildViewAttachedToWindow(View view) {
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
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
         * Called when a drag has started for {@link RecyclerView.ViewHolder}.
         */
        void onDragStarted(RecyclerView.ViewHolder holder);

        /**
         * Called to determine whether {@code holder} can be swapped with {@code target}.
         * If true, {@link #onSwap(RecyclerView.ViewHolder, RecyclerView.ViewHolder)} might be called in the future.
         */
        boolean canSwap(RecyclerView.ViewHolder holder, RecyclerView.ViewHolder target);

        /**
         * Called to swap {@code holder} to {@code target}'s adapter position and notify the {@link
         * RecyclerView.Adapter}.
         */
        void onSwap(RecyclerView.ViewHolder holder, RecyclerView.ViewHolder target);

        /**
         * Called when a drag has ended for {@link RecyclerView.ViewHolder}, after any recovery animations end.
         */
        void onDragStopped(RecyclerView.ViewHolder holder);
    }
}