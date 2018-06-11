package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

/**
 * Keeps track of animation ids and change ids so that calculating operations between two lists (old and new) is
 * possible even if the user does not always use {@link AnimatedAdapter#animateDataSetChanged()}.
 */
class ItemsObserver extends RecyclerView.AdapterDataObserver {
    private final Items items;
    private final DataSetDiffer.Callback callback;

    public ItemsObserver(Items items, DataSetDiffer.Callback callback) {
        this.items = items;
        this.callback = callback;
    }

    @Override
    public void onChanged() {
        int itemCount = callback.getItemCount();
        items.clear();
        items.ensureCapacity(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(callback.getItemId(i), callback.getItemContentHash(i));
        }
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        for (int i = positionStart; i < positionStart + itemCount; i++) {
            items.setContentHash(i, callback.getItemContentHash(i));
        }
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        items.ensureCapacity(items.size() + itemCount);
        for (int i = positionStart; i < positionStart + itemCount; i++) {
            items.add(i, callback.getItemId(i), callback.getItemContentHash(i));
        }
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        items.remove(positionStart, positionStart + itemCount);
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        boolean incrementPositions = fromPosition > toPosition;
        for (int i = 0; i < itemCount; i++) {
            long id = items.getId(fromPosition);
            long changeHash = items.getContentHash(fromPosition);
            items.remove(fromPosition);
            items.add(toPosition, id, changeHash);
            if (incrementPositions) {
                fromPosition++;
                toPosition++;
            }
        }
    }
}
