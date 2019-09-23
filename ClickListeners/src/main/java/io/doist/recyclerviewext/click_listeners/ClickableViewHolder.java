package io.doist.recyclerviewext.click_listeners;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ClickableViewHolder extends RecyclerView.ViewHolder {
    public ClickableViewHolder(View itemView, @Nullable OnItemClickListener onItemClickListener) {
        this(itemView, onItemClickListener, null);
    }

    public ClickableViewHolder(View itemView, @Nullable OnItemClickListener onItemClickListener,
                               @Nullable OnItemLongClickListener onItemLongClickListener) {
        super(itemView);

        setClickListeners(onItemClickListener, onItemLongClickListener);
    }

    private void setClickListeners(@Nullable final OnItemClickListener onItemClickListener,
                                   @Nullable final OnItemLongClickListener onItemLongClickListener) {
        if (onItemClickListener != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(ClickableViewHolder.this);
                }
            });
        }

        if (onItemLongClickListener != null) {
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onItemLongClickListener.onItemLongClick(ClickableViewHolder.this);
                    return true;
                }
            });
        }
    }
}
