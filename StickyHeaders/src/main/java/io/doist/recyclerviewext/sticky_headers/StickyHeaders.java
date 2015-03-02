package io.doist.recyclerviewext.sticky_headers;

import android.support.v7.widget.RecyclerView;

/**
 * Adds sticky headers capabilities to the {@link RecyclerView.Adapter}. Should return {@code true} for all
 * positions that represent sticky headers.
 */
public interface StickyHeaders {
    public boolean isHeader(int position);
}
