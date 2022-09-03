package io.doist.recyclerviewext.choice_modes;

import android.widget.AbsListView;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Tracks multiple selections, similarly to {@link AbsListView#CHOICE_MODE_MULTIPLE}. Calls to
 * {@link RecyclerView.Adapter#notifyItemChanged(int)} are performed automatically.
 *
 * Optionally, call {@link #bind(RecyclerView.ViewHolder, boolean)} from your
 * {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)} to have the
 * {@link android.R.attr#state_activated} reflect the selected state.
 */
public class MultiSelector extends Selector {
    private final Set<String> mSelectedIds = new LinkedHashSet<>();

    public MultiSelector(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.Adapter adapter) {
        super(recyclerView, adapter);
    }

    public void setSelected(@NonNull String id, boolean selected) {
        boolean changed;
        String[] previousSelectedIds = getSelectedIds();
        if (selected) {
            changed = mSelectedIds.add(id);
        } else {
            changed = mSelectedIds.remove(id);
        }

        if (changed) {
            notifyItemChanged(id);

            onSelectionChanged(getSelectedIds(), previousSelectedIds);
        }
    }

    public boolean isSelected(@NonNull String id) {
        return mSelectedIds.contains(id);
    }

    @Override
    public String[] getSelectedIds() {
        String[] selectedIds = new String[mSelectedIds.size()];
        int i = 0;
        for (String selectedId : mSelectedIds) {
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
        String[] previousSelectedIds = getSelectedIds();
        boolean hadSelections = getSelectedCount() > 0;

        Iterator<String> it = mSelectedIds.iterator();
        while (it.hasNext()) {
            String selectedId = it.next();
            it.remove();

            notifyItemChanged(selectedId);
        }

        onSelectionChanged(getSelectedIds(), previousSelectedIds);
    }
}
