package io.doist.recyclerviewext.flippers;

import android.app.ListActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Handles a {@link RecyclerView}'s empty view, similar to the automatic control provided by {@link ListActivity} for
 * {@link ListView}.
 */
public class EmptyRecyclerFlipper extends Flipper {
    protected RecyclerView mRecyclerView;
    protected View mEmptyView;

    private RecyclerView.Adapter mAdapter;
    private int mCount;

    private RecyclerView.AdapterDataObserver mObserver = new EmptyAdapterDataObserver();

    public EmptyRecyclerFlipper(ViewGroup container) {
        this(container, android.R.id.list, android.R.id.empty);
    }

    public EmptyRecyclerFlipper(ViewGroup container, @IdRes int recyclerViewId, @IdRes int emptyViewId) {
        this((RecyclerView) container.findViewById(recyclerViewId), container.findViewById(emptyViewId));
    }

    public EmptyRecyclerFlipper(RecyclerView recyclerView, View emptyView) {
        mRecyclerView = recyclerView;
        mEmptyView = emptyView;
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
                    replaceNoAnimation(mEmptyView, mRecyclerView);
                } else {
                    replaceNoAnimation(mRecyclerView, mEmptyView);
                }
            }
            return true;
        }
        return false;
    }

    private void disableItemAnimatorForRecyclerInAnimation() {
        final RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        FlipperAnimator flipperAnimator = getFlipperAnimator();
        if (itemAnimator != null && flipperAnimator != null) {
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.setItemAnimator(itemAnimator);
                }
            }, flipperAnimator.getDuration());
        }
    }

    @Override
    protected void replaceInternal(View outView, View inView, boolean animate) {
        if (animate && inView == mRecyclerView) {
            disableItemAnimatorForRecyclerInAnimation();
        }
        super.replaceInternal(outView, inView, animate);
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
                replace(mRecyclerView, mEmptyView);
            } else if (count > 0 && mCount == 0) {
                replace(mEmptyView, mRecyclerView);
            }
            mCount = count;
        }
    }
}
