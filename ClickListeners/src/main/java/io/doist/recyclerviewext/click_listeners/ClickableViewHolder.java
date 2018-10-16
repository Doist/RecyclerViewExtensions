package io.doist.recyclerviewext.click_listeners;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class ClickableViewHolder extends RecyclerView.ViewHolder {
    public ClickableViewHolder(View itemView, OnItemClickListener onItemClickListener) {
        this(itemView, onItemClickListener, null);
    }

    public ClickableViewHolder(View itemView, OnItemClickListener onItemClickListener,
                               OnItemLongClickListener onItemLongClickListener) {
        super(itemView);

        setClickListeners(onItemClickListener, onItemLongClickListener);
    }

    private void setClickListeners(final OnItemClickListener onItemClickListener,
                                   final OnItemLongClickListener onItemLongClickListener) {
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
