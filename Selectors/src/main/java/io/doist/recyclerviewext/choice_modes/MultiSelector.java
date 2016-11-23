package io.doist.recyclerviewext.choice_modes;

import android.support.v7.widget.RecyclerView;
import android.widget.AbsListView;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks multiple selections, similarly to {@link AbsListView#CHOICE_MODE_MULTIPLE}. Calls to
 * {@link RecyclerView.Adapter#notifyItemChanged(int)} are performed automatically.
 *
 * Optionally, call {@link #bind(RecyclerView.ViewHolder)} from your
 * {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)} to have the
 * {@link android.R.attr#state_activated} reflect the selected state.
 */
public class MultiSelector extends Selector {
    private Set<Long> mSelectedIds = new LinkedHashSet<>();

    public MultiSelector(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        super(recyclerView, adapter);
    }

    public void setSelected(long id, boolean selected) {
        boolean changed;
        if (selected) {
            changed = mSelectedIds.add(id);
        } else {
            changed = mSelectedIds.remove(id);
        }

        if (changed) {
            notifyItemChanged(id);

            if (mObserver != null) {
                mObserver.onSelectionChanged(this);
            }
        }
    }

    public boolean isSelected(long id) {
        return mSelectedIds.contains(id);
    }

    @Override
    public long[] getSelectedIds() {
        long[] selectedIds = new long[mSelectedIds.size()];
        int i = 0;
        for (Long selectedId : mSelectedIds) {
            selectedIds[i++] = selectedId;
        }
        return selectedIds;
    }

    @Override
    public int getSelectedCount() {
        return mSelectedIds.size();
    }

    @Override
    public void clearSelected() {
        clearSelected(true);
    }

    @Override
    protected void clearSelected(boolean notify) {
        boolean hadSelections = mSelectedIds.size() > 0;

        Iterator<Long> it = mSelectedIds.iterator();
        while (it.hasNext()) {
            Long selectedId = it.next();
            it.remove();
            if (notify) {
                notifyItemChanged(selectedId);
            }
        }

        if (hadSelections && mObserver != null) {
            mObserver.onSelectionChanged(this);
        }
    }
}
