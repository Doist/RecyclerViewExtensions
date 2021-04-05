package io.doist.recyclerviewext.flippers;

import android.app.ListActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Handles a {@link RecyclerView}'s empty view, similar to the automatic control provided by {@link
 * ListActivity} for {@link ListView}.
 * <p>
 * EmptyRecyclerFlipper will stop being a subclass of {@link Flipper}. Flipper used on the {@link
 * EmptyRecyclerFlipper(RecyclerView, View, Flipper)} should do all the customizations of
 * transitions.
 */
@SuppressWarnings("rawtypes")
public class EmptyRecyclerFlipper extends Flipper {
    private final RecyclerView mRecyclerView;
    private final View mEmptyView;
    private final Flipper mFlipper;

    private RecyclerView.Adapter mAdapter;
    private int mCount;

    private final RecyclerView.AdapterDataObserver mObserver = new EmptyAdapterDataObserver();

    public EmptyRecyclerFlipper(ViewGroup container) {
        this(container, android.R.id.list, android.R.id.empty);
    }

    public EmptyRecyclerFlipper(ViewGroup container,
                                @IdRes int recyclerViewId,
                                @IdRes int emptyViewId) {
        this((RecyclerView) container.findViewById(recyclerViewId),
             container.findViewById(emptyViewId), new Flipper());
    }

    public EmptyRecyclerFlipper(ViewGroup container,
                                @IdRes int recyclerViewId,
                                @IdRes int emptyViewId,
                                Flipper flipper) {
        this((RecyclerView) container.findViewById(recyclerViewId),
             container.findViewById(emptyViewId), flipper);
    }

    public EmptyRecyclerFlipper(RecyclerView recyclerView, View emptyView) {
        this(recyclerView, emptyView, new Flipper());
    }

    public EmptyRecyclerFlipper(RecyclerView recyclerView, View emptyView, Flipper flipper) {
        mRecyclerView = recyclerView;
        mEmptyView = emptyView;
        mFlipper = flipper;
    }

    public boolean monitor(RecyclerView.Adapter adapter) {
        // Start monitoring this adapter by registering an observer and ensuring the current visibility state is valid.
        if (adapter != mAdapter) {
            if (mAdapter != null) {
                mAdapter.unregisterAdapterDataObserver(mObserver);
            }
            mAdapter = adapter;
            if (mAdapter != null) {
                mAdapter.registerAdapterDataObserver(mObserver);
                mCount = mAdapter.getItemCount();
                if (mCount > 0) {
                    showContent(false);
                } else {
                    showEmpty(false);
                }
            }

            return true;
        }

        return false;
    }

    protected void showContent(boolean animate) {
        executeFlip(mEmptyView, mRecyclerView, animate);
    }

    protected void showEmpty(boolean animate) {
        executeFlip(mRecyclerView, mEmptyView, animate);
    }

    protected void onFlipCompleted(View outView, View inView, boolean animate) {
        if (animate && inView == mRecyclerView) {
            disableItemAnimatorForRecyclerInAnimation();
        }
    }

    protected void executeFlip(final View outView, final View inView, final boolean animate) {
        if (animate) {
            mFlipper.replace(outView, inView, new Runnable() {
                @Override
                public void run() {
                    onFlipCompleted(outView, inView, true);
                }
            });
        } else {
            mFlipper.replaceNoAnimation(outView, inView, new Runnable() {
                @Override
                public void run() {
                    onFlipCompleted(outView, inView, false);
                }
            });
        }
    }

    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    protected View getEmptyView() {
        return mEmptyView;
    }

    protected Flipper getFlipper() {
        return mFlipper;
    }

    /**
     * @deprecated To customize the flipper use the {@link #getFlipper()}.
     */
    @Override
    public FlipperAnimator getFlipperAnimator() {
        return mFlipper.getFlipperAnimator();
    }

    /**
     * @deprecated To customize the flipper use the {@link #getFlipper()}.
     */
    @Override
    public void setFlipperAnimator(FlipperAnimator flipperAnimator) {
        mFlipper.setFlipperAnimator(flipperAnimator);
    }

    @Override
    public void replace(View outView, View inView) {
        mFlipper.replace(outView, inView);
    }

    @Override
    public void replace(View outView, View inView, Runnable afterReplaceAction) {
        mFlipper.replace(outView, inView, afterReplaceAction);
    }

    @Override
    public void replaceNoAnimation(View outView, View inView) {
        mFlipper.replaceNoAnimation(outView, inView);
    }

    @Override
    public void replaceNoAnimation(View outView, View inView, Runnable afterReplaceAction) {
        mFlipper.replaceNoAnimation(outView, inView, afterReplaceAction);
    }

    @Override
    protected void replaceInternal(View outView, View inView, boolean animate) {
        mFlipper.replaceInternal(outView, inView, animate);
    }

    private void disableItemAnimatorForRecyclerInAnimation() {
        final RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator != null) {
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.setItemAnimator(itemAnimator);
                }
            }, mFlipper.getFlipperAnimator().getDuration());
        }
    }

    private class EmptyAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            checkCount();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkCount();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkCount();
        }

        private void checkCount() {
            int count = mAdapter.getItemCount();
            if (count == 0 && mCount > 0) {
                showEmpty(true);
            } else if (count > 0 && mCount == 0) {
                showContent(true);
            }
            mCount = count;
        }
    }
}
