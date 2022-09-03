package io.doist.recyclerviewext.choice_modes;

import android.widget.AbsListView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Tracks multiple selections, similarly to {@link AbsListView#CHOICE_MODE_SINGLE}. Calls to
 * {@link RecyclerView.Adapter#notifyItemChanged(int)} are done automatically.
 * <p>
 * Optionally, call {@link #bind(RecyclerView.ViewHolder, boolean)} from your
 * {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)} to have the
 * {@link android.R.attr#state_activated} reflect the selected state.
 */
public class SingleSelector extends Selector {
    private String mSelectedId;
    private boolean mSelected;

    public SingleSelector(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.Adapter adapter) {
        super(recyclerView, adapter);
    }

    public void setSelected(@NonNull String id, boolean selected) {
        String[] previousSelectedIds = getSelectedIds();
        boolean oldSelected = mSelected;
        String oldSelectedId = mSelectedId;

        mSelected = selected;
        if (mSelected) {
            mSelectedId = id;
        }

        if (mSelected != oldSelected || !Objects.equals(oldSelectedId, mSelectedId)) {
            if (oldSelected) {
                notifyItemChanged(oldSelectedId);
            }
            if (mSelected) {
                notifyItemChanged(mSelectedId);
            }

            onSelectionChanged(getSelectedIds(), previousSelectedIds);
        }
    }

    public boolean isSelected(@NonNull String id) {
        return mSelected && Objects.equals(mSelectedId, id);
    }

    @Override
    public String[] getSelectedIds() {
        return mSelected ? new String[]{mSelectedId} : new String[0];
    }

    @Override
    public int getSelectedCount() {
        return mSelected ? 1 : 0;
    }

    @Override
    public void clearSelected() {
        String[] previousSelectedIds = getSelectedIds();
        boolean hadSelection = mSelected;

        mSelected = false;

        if (hadSelection) {
            notifyItemChanged(mSelectedId);

            onSelectionChanged(getSelectedIds(), previousSelectedIds);
        }
    }
}
