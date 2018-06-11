package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

/**
 * Wrapper around {@link DataSetDiffer}, which adds functionality to animate between data sets.
 *
 * To seamlessly animate between data sets, call {@link #animateDataSetChanged()} in place of
 * {@link #notifyDataSetChanged()}.
 */
public abstract class AnimatedAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements DataSetDiffer.Callback {

    private DataSetDiffer dataSetDiffer;

    protected AnimatedAdapter() {
        setAnimationsEnabled(true);
    }

    /**
     * @see DataSetDiffer.Callback#getItemContentHash(int)
     */
    public abstract long getItemContentHash(int position);

    /**
     * Returns whether animations are enabled or not.
     */
    public final boolean areAnimationsEnabled() {
        return dataSetDiffer != null;
    }

    /**
     * Sets whether animations are enabled or not (enabled by default).
     *
     * If set to {@code false}, {@link #animateDataSetChanged()} proxies to {@link #notifyDataSetChanged()}.
     */
    public final void setAnimationsEnabled(boolean enabled) {
        if (enabled && dataSetDiffer == null) {
            dataSetDiffer = new DataSetDiffer(this, this);
        } else if (!enabled && dataSetDiffer != null) {
            dataSetDiffer.stopObservingItems();
            dataSetDiffer = null;
        }
    }

    /**
     * Analyzes the data set using {@link #getItemId(int)} and {@link #getItemContentHash(int)} and calls the
     * necessary {@code notify*} methods to go from the previous data set to the new one.
     *
     * This method should be called right after the data set is updated.
     */
    public void animateDataSetChanged() {
        if (areAnimationsEnabled()) {
            dataSetDiffer.diffDataSet();
        } else {
            notifyDataSetChanged();
        }
    }
}
