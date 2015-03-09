package io.doist.recyclerviewext.flippers;

import android.app.ListActivity;
import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Handles a {@link RecyclerView}'s empty view, similar to the automatic control provided by {@link ListActivity} for
 * {@link ListView}.
 */
public class EmptyRecyclerFlipper extends Flipper {
    protected View mRecyclerView;
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

        // Grab add / remove ItemAnimator duration (equal in DefaultItemAnimator) as our animator's time.
        RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
        if (itemAnimator != null) {
            long duration = Math.max(itemAnimator.getAddDuration(), itemAnimator.getRemoveDuration());
            if (duration > 0) {
                getFlipperAnimator().setFlipDuration(duration);
            }
        }
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

    private class EmptyAdapterDataObserver extends RecyclerView.AdapterDataObserver implements Runnable {
        private boolean mScheduledCheck = false;

        @Override
        public void onChanged() {
            postRunIfWindowVisible();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            postRunIfWindowVisible();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            postRunIfWindowVisible();
        }

        private void postRunIfWindowVisible() {
            if (mRecyclerView.getWindowVisibility() == View.VISIBLE) {
                // Adjust visibility in a post to let all notify* calls go through.
                if (!mScheduledCheck) {
                    mRecyclerView.post(this);
                    mScheduledCheck = true;
                }
            } else {
                // Window is not visible yet, so animations won't run. Adjust visibility now.
                run();
            }
        }

        @Override
        public void run() {
            int count = mAdapter.getItemCount();
            if (count == 0 && mCount > 0) {
                replace(mRecyclerView, mEmptyView);
            } else if(count > 0 && mCount == 0) {
                replace(mEmptyView, mRecyclerView);
            }
            mCount = count;
            mScheduledCheck = false;
        }
    }
}
