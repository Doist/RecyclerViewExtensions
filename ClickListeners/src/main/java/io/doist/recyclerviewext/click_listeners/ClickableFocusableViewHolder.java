package io.doist.recyclerviewext.click_listeners;

import android.view.View;

public class ClickableFocusableViewHolder extends ClickableViewHolder {
    public ClickableFocusableViewHolder(View itemView, OnItemClickListener onItemClickListener) {
        super(itemView, onItemClickListener, null);

        setFocusable(onItemClickListener, null);
    }

    public ClickableFocusableViewHolder(View itemView, OnItemClickListener onItemClickListener,
                                        OnItemLongClickListener onItemLongClickListener) {
        super(itemView, onItemClickListener, onItemLongClickListener);

        setFocusable(onItemClickListener, onItemLongClickListener);
    }

    private void setFocusable(OnItemClickListener onItemClickListener,
                              OnItemLongClickListener onItemLongClickListener) {
        itemView.setFocusable(onItemClickListener != null || onItemLongClickListener != null);
    }
}
