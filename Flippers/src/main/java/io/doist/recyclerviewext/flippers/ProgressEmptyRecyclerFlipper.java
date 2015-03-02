package io.doist.recyclerviewext.flippers;

import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Similar to {@link EmptyRecyclerFlipper}, but also handles a progress view through {@link #setLoading(boolean)}.
 */
public class ProgressEmptyRecyclerFlipper extends EmptyRecyclerFlipper {
    private int mProgressViewId;
    private int mCurrentViewId;
    private boolean mLoading;

    public ProgressEmptyRecyclerFlipper(ViewGroup container) {
        this(container, android.R.id.list, android.R.id.empty, android.R.id.progress);
    }

    public ProgressEmptyRecyclerFlipper(ViewGroup container, @IdRes int progressViewId) {
        this(container, android.R.id.list, android.R.id.empty, progressViewId);
    }

    public ProgressEmptyRecyclerFlipper(ViewGroup container, @IdRes int recyclerViewId, @IdRes int emptyViewId,
                                        @IdRes int progressViewId) {
        super(container, recyclerViewId, emptyViewId);
        init(container, recyclerViewId, emptyViewId, progressViewId);
    }

    @Override
    public boolean monitor(RecyclerView.Adapter adapter) {
        boolean monitored = super.monitor(adapter);
        if (monitored) {
            mContainer.findViewById(mProgressViewId).setVisibility(View.GONE);
        }
        return monitored;
    }

    private void init(ViewGroup container, int recyclerViewId, int emptyViewId, int loadingViewId) {
        mProgressViewId = loadingViewId;
        container.findViewById(mProgressViewId).setVisibility(View.GONE);
        mCurrentViewId =
                container.findViewById(recyclerViewId).getVisibility() == View.VISIBLE ? recyclerViewId : emptyViewId;
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
                super.replaceInternal(mCurrentViewId, mProgressViewId, animate);
            } else {
                super.replaceInternal(mProgressViewId, mCurrentViewId, animate);
            }
        }
    }

    @Override
    protected void replaceInternal(@IdRes int outId, @IdRes int inId, boolean animate) {
        if (!mLoading) {
            super.replaceInternal(outId, inId, animate);
        }
        mCurrentViewId = inId;
    }
}
