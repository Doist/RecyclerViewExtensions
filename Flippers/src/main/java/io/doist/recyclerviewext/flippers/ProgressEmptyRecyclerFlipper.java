package io.doist.recyclerviewext.flippers;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Similar to {@link EmptyRecyclerFlipper}, but also handles a progress view through {@link #setLoading(boolean)}.
 */
public class ProgressEmptyRecyclerFlipper extends EmptyRecyclerFlipper {
    private View mProgressView;
    private View mCurrentView;
    private boolean mLoading;

    public ProgressEmptyRecyclerFlipper(ViewGroup container) {
        this(container, android.R.id.list, android.R.id.empty, android.R.id.progress);
    }

    public ProgressEmptyRecyclerFlipper(ViewGroup container, @IdRes int progressViewId) {
        this(container, android.R.id.list, android.R.id.empty, progressViewId);
    }

    public ProgressEmptyRecyclerFlipper(ViewGroup container, @IdRes int recyclerViewId, @IdRes int emptyViewId,
                                        @IdRes int progressViewId) {
        this((RecyclerView) container.findViewById(recyclerViewId),
             container.findViewById(emptyViewId),
             container.findViewById(progressViewId));
    }

    public ProgressEmptyRecyclerFlipper(RecyclerView recyclerView, View emptyView, View progressView) {
        super(recyclerView, emptyView);
        mProgressView = progressView;
        mCurrentView = recyclerView.getVisibility() == View.VISIBLE ? recyclerView : emptyView;
    }

    public void setLoading(boolean loading) {
        setLoading(loading, true);
    }

    public void setLoadingNoAnimation(boolean loading) {
        setLoading(loading, false);
    }

    private void setLoading(boolean loading, boolean animate) {
        if (mLoading != loading) {
            mLoading = loading;
            if (mLoading) {
                super.replaceInternal(mCurrentView, mProgressView, animate);
            } else {
                super.replaceInternal(mProgressView, mCurrentView, animate);
            }
        }
    }

    @Override
    public boolean monitor(RecyclerView.Adapter adapter) {
        boolean monitored = super.monitor(adapter);
        if (monitored) {
            mProgressView.setVisibility(View.GONE);
            mLoading = false;
        }
        return monitored;
    }

    @Override
    protected void replaceInternal(View outView, View inView, boolean animate) {
        if (!mLoading) {
            super.replaceInternal(outView, inView, animate);
        }
        mCurrentView = inView;
    }
}
