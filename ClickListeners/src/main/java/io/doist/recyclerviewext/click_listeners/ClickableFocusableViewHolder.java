package io.doist.recyclerviewext.click_listeners;

import android.view.View;

import androidx.annotation.Nullable;

public class ClickableFocusableViewHolder extends ClickableViewHolder {
    public ClickableFocusableViewHolder(View itemView, @Nullable OnItemClickListener onItemClickListener) {
        this(itemView, onItemClickListener, null);
    }

    public ClickableFocusableViewHolder(View itemView, @Nullable OnItemClickListener onItemClickListener,
                                        @Nullable OnItemLongClickListener onItemLongClickListener) {
        super(itemView, onItemClickListener, onItemLongClickListener);

        itemView.setFocusable(onItemClickListener != null || onItemLongClickListener != null);
    }
}
