package io.doist.recyclerviewext.flippers;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Similar to {@link EmptyRecyclerFlipper}, but also handles a progress view through {@link
 * #setLoading(boolean)}.
 */
public class ProgressEmptyRecyclerFlipper extends EmptyRecyclerFlipper {
    private final View mProgressView;
    private View mCurrentView;
    private boolean mLoading;

    public ProgressEmptyRecyclerFlipper(ViewGroup container) {
        this(container, android.R.id.list, android.R.id.empty, android.R.id.progress);
    }

    public ProgressEmptyRecyclerFlipper(ViewGroup container, @IdRes int progressViewId) {
        this(container, android.R.id.list, android.R.id.empty, progressViewId);
    }

    public ProgressEmptyRecyclerFlipper(ViewGroup container,
                                        @IdRes int recyclerViewId,
                                        @IdRes int emptyViewId,
                                        @IdRes int progressViewId,
                                        Flipper flipper) {
        this((RecyclerView) container.findViewById(recyclerViewId),
             container.findViewById(emptyViewId),
             container.findViewById(progressViewId),
             flipper
            );
    }

    public ProgressEmptyRecyclerFlipper(ViewGroup container,
                                        @IdRes int recyclerViewId,
                                        @IdRes int emptyViewId,
                                        @IdRes int progressViewId) {
        this((RecyclerView) container.findViewById(recyclerViewId),
             container.findViewById(emptyViewId),
             container.findViewById(progressViewId),
             new Flipper()
            );
    }

    public ProgressEmptyRecyclerFlipper(RecyclerView recyclerView,
                                        View emptyView,
                                        View progressView) {
        this(recyclerView, emptyView, progressView, new Flipper());
    }

    public ProgressEmptyRecyclerFlipper(RecyclerView recyclerView,
                                        View emptyView,
                                        View progressView,
                                        Flipper flipper) {
        super(recyclerView, emptyView, flipper);

        mProgressView = progressView;
        mCurrentView = recyclerView.getVisibility() == View.VISIBLE ? recyclerView : emptyView;
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

    public void setLoading(boolean isLoading) {
        setLoading(isLoading, true);
    }

    public void setLoadingNoAnimation(boolean isLoading) {
        setLoading(isLoading, false);
    }

    @Override
    protected void onFlipCompleted(View outView, View inView, boolean animate) {
        if (!mLoading) {
            super.onFlipCompleted(outView, inView, animate);
        }
        mCurrentView = inView;
    }

    protected void onLoadingFlipped(View outView, View inView, boolean animate) {
        super.onFlipCompleted(outView, inView, animate);

        if (mCurrentView != getRecyclerView()) {
            getRecyclerView().setVisibility(View.GONE);
        }
        if (mCurrentView != getEmptyView()) {
            getEmptyView().setVisibility(View.GONE);
        }
    }

    protected View getProgressView() {
        return mProgressView;
    }

    protected View getCurrentView() {
        return mCurrentView;
    }

    protected void setCurrentView(View currentView) {
        mCurrentView = currentView;
    }

    protected void setLoadingVisible(boolean isLoading) {
        mLoading = isLoading;
    }

    protected boolean isLoadingVisible() {
        return mLoading;
    }

    private void setLoading(boolean loading, boolean animate) {
        if (mLoading != loading) {
            mLoading = loading;
            if (mLoading) {
                showLoading(animate);
            } else {
                hideLoading(animate);
            }
        }
    }

    private void showLoading(final boolean animate) {
        flipLoading(mCurrentView, mProgressView, animate);
    }

    private void hideLoading(final boolean animate) {
        flipLoading(mProgressView, mCurrentView, animate);
    }

    private void flipLoading(final View outView, final View inView, final boolean animate) {
        if (animate) {
            getFlipper().replace(outView, inView, new Runnable() {
                @Override
                public void run() {
                    onLoadingFlipped(outView, inView, true);
                }
            });
        } else {
            getFlipper().replaceNoAnimation(outView, inView, new Runnable() {
                @Override
                public void run() {
                    onLoadingFlipped(outView, inView, false);
                }
            });
        }
    }
}
