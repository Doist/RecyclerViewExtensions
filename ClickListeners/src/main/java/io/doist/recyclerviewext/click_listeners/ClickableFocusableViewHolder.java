package io.doist.recyclerviewext.click_listeners;

import android.view.View;

public class ClickableFocusableViewHolder extends ClickableViewHolder {
    public ClickableFocusableViewHolder(View itemView, OnItemClickListener onItemClickListener) {
        super(itemView, onItemClickListener);
        itemView.setFocusable(onItemClickListener != null);
    }
}
