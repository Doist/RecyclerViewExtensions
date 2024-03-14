package io.doist.recyclerviewext.sticky_headers;

import androidx.recyclerview.widget.RecyclerView;

public interface StickyHeaderProvider {

    boolean isStickyHeader(RecyclerView.Adapter<?> adapter, int position);

}
