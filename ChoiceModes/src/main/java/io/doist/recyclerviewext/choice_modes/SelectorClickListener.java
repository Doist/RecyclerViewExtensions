package io.doist.recyclerviewext.choice_modes;

import android.view.View;

import io.doist.recyclerviewext.click_listeners.OnItemClickListener;

public class SelectorClickListener implements OnItemClickListener {
    private Selector mSelector;

    public SelectorClickListener(Selector selector) {
        mSelector = selector;
    }

    @Override
    public void onItemClick(View view, int position, long id) {
        mSelector.toggleSelected(id);
    }
}
