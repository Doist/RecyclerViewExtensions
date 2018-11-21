package io.doist.recyclerviewext.demo;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

abstract class BindableViewHolder extends RecyclerView.ViewHolder {
    public BindableViewHolder(View itemView) {
        super(itemView);
    }

    protected abstract void bind(Object object);
}
