package io.doist.recyclerviewext.flippers;

import android.app.ListActivity;
import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Handles a {@link RecyclerView}'s empty view, similar to the automatic control provided by {@link ListActivity} for
 * {@link ListView}.
 */
public class EmptyRecyclerFlipper extends Flipper {
    protected int mRecyclerViewId;
    protected int mEmptyViewId;

    private RecyclerView.Adapter mAdapter;
    private int mCount;

    private RecyclerView.AdapterDataObserver mObserver = new EmptyAdapterDataObserver();

    public EmptyRecyclerFlipper(ViewGroup container) {
        this(container, android.R.id.list, android.R.id.empty);
    }

    public EmptyRecyclerFlipper(ViewGroup container, @IdRes int recyclerViewId, @IdRes int emptyViewId) {
        super(container);
        mRecyclerViewId = recyclerViewId;
        mEmptyViewId = emptyViewId;
    }

    public boolean monitor(RecyclerView.Adapter adapter) {
        if (adapter != mAdapter) {
            if (mAdapter != null) {
                mAdapter.unregisterAdapterDataObserver(mObserver);
            }
            mAdapter = adapter;
            if (mAdapter != null) {
                mAdapter.registerAdapterDataObserver(mObserver);
                mCount = mAdapter.getItemCount();
                if (mCount > 0) {
                    replaceInternal(mEmptyViewId, mRecyclerViewId, false);
                } else {
                    replaceInternal(mRecyclerViewId, mEmptyViewId, false);
                }
            }
            return true;
        }
        return false;
    }

    private class EmptyAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            mCount = mAdapter.getItemCount();
            checkForEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mCount += itemCount;
            if (mCount == itemCount) {
                checkForEmpty();
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mCount -= itemCount;
            if (mCount == 0) {
                checkForEmpty();
            }
        }

        private void checkForEmpty() {
            if (mCount == 0) {
                replace(mRecyclerViewId, mEmptyViewId);
            } else {
                replace(mEmptyViewId, mRecyclerViewId);
            }
        }
    }
}
