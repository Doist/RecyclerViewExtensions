package io.doist.recyclerviewext.click_listeners;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ClickableViewHolder extends RecyclerView.ViewHolder {
    public ClickableViewHolder(View itemView, final OnItemClickListener onItemClickListener) {
        super(itemView);

        if (onItemClickListener != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(v, getLayoutPosition(), getItemId());
                }
            });
        }
    }
}
