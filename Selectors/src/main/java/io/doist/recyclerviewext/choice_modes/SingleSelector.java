package io.doist.recyclerviewext.choice_modes;

import android.support.v7.widget.RecyclerView;
import android.widget.AbsListView;

/**
 * Tracks multiple selections, similarly to {@link AbsListView#CHOICE_MODE_SINGLE}. Calls to
 * {@link RecyclerView.Adapter#notifyItemChanged(int)} are done automatically.
 *
 * Optionally, call {@link #bind(RecyclerView.ViewHolder)} from your
 * {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)} to have the
 * {@link android.R.attr#state_activated} reflect the selected state.
 */
public class SingleSelector extends Selector {
    private long mSelectedId;
    private boolean mSelected;

    public SingleSelector(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        super(recyclerView, adapter);
    }

    public void setSelected(long id, boolean selected) {
        boolean oldSelected = mSelected;
        long oldSelectedId = mSelectedId;

        mSelected = selected;
        if (mSelected) {
            mSelectedId = id;
        }

        if(mSelected != oldSelected || oldSelectedId != mSelectedId) {
            if (mObserver != null) {
                mObserver.onSelectionChanged(this);
            }

            if (oldSelected) {
                notifyItemChangedIfVisible(oldSelectedId);
            }
            if (mSelected) {
                notifyItemChangedIfVisible(mSelectedId);
            }
        }
    }

    public boolean isSelected(long id) {
        return mSelected && mSelectedId == id;
    }

    @Override
    public long[] getSelectedIds() {
        return mSelected ? new long[]{mSelectedId} : new long[0];
    }

    @Override
    public int getSelectedCount() {
        return mSelected ? 1 : 0;
    }

    @Override
    public void clearSelected() {
        clearSelected(true);
    }

    @Override
    protected void clearSelected(boolean notify) {
        boolean selected = mSelected;
        mSelected = false;
        if (selected) {
            if (mObserver != null) {
                mObserver.onSelectionChanged(this);
            }
            if (notify) {
                notifyItemChangedIfVisible(mSelectedId);
            }
        }
    }
}
