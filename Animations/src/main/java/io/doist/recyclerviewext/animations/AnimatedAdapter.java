package io.doist.recyclerviewext.animations;

import androidx.recyclerview.widget.RecyclerView;

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
     * @deprecated Use {@link #animateDataSetChanged(Object, Object)}.
     * It fixes cases when the list instance is changed but the contents are the same.
     */
    @Deprecated
    public void animateDataSetChanged() {
        animateDataSetChanged(null, null);
    }

    /**
     * This method should be called right after the data set is updated.
     */
    public void animateDataSetChanged(Object oldListObj, Object newListObj) {
        if (areAnimationsEnabled()) {
            dataSetDiffer.diffDataSet(oldListObj, newListObj);
        } else {
            notifyDataSetChanged();
        }
    }
}
