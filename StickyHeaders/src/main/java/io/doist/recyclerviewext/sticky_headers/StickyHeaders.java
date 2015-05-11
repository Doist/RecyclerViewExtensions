package io.doist.recyclerviewext.sticky_headers;

import android.support.v7.widget.RecyclerView;

/**
 * Adds sticky headers capabilities to the {@link RecyclerView.Adapter}. Should return {@code true} for all
 * positions that represent sticky headers.
 */
public interface StickyHeaders {
    boolean isStickyHeader(int position);

    interface ViewSetup {
        /**
         * Adjusts any necessary properties of the {@code holder} that is being used as a sticky header.
         *
         * {@link #teardownStickyHeaderViewHolder(RecyclerView.ViewHolder)} will be called sometime after this method
         * and before any other calls to this method go through.
         */
        void setupStickyHeaderViewHolder(RecyclerView.ViewHolder holder);

        /**
         * Reverts any properties changed in {@link #setupStickyHeaderViewHolder(RecyclerView.ViewHolder)}.
         *
         * Called after {@link #setupStickyHeaderViewHolder(RecyclerView.ViewHolder)}.
         */
        void teardownStickyHeaderViewHolder(RecyclerView.ViewHolder holder);
    }
}
