package io.doist.recyclerviewext.demo;

import android.support.v7.widget.RecyclerView;
import android.view.View;

abstract class BindableViewHolder extends RecyclerView.ViewHolder {
    public BindableViewHolder(View itemView) {
        super(itemView);
    }

    protected abstract void bind(Object object);
}
