package io.doist.recyclerviewext.click_listeners;

import android.view.View;

public class ClickableFocusableViewHolder extends ClickableViewHolder {
    public ClickableFocusableViewHolder(View itemView, OnItemClickListener onItemClickListener) {
        this(itemView, onItemClickListener, null);
    }

    public ClickableFocusableViewHolder(View itemView, OnItemClickListener onItemClickListener,
                                        OnItemLongClickListener onItemLongClickListener) {
        super(itemView, onItemClickListener, onItemLongClickListener);

        itemView.setFocusable(onItemClickListener != null || onItemLongClickListener != null);
    }
}
