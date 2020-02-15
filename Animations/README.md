# Animations

[RecyclerViewExtensions](https://github.com/Doist/RecyclerViewExtensions) module providing utilities for animations.

## `AnimatedAdapter`

`Adapter` implementation that provides an `animateDataSetChanges()` method to animate between two data sets. It uses `DataSetDiffer`.

To animate data set changes, `AnimatedAdapter` requires [stable ids](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter.html#setHasStableIds(boolean)) and the following method implementations:
* `getItemId(int)`, providing ids for each item. As per the [documentation](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter.html#hasStableIds()), these must be unique.
* `getItemContentHash(int)`, providing a hash comprised of the item properties bound in the adapter. All mutable state that affects the views when binding should be part of the hash input, that any changes are properly detected.

Provided these, calling `animateDataSetChanges()` when the adapter contents change will animate between the previous and current data sets.

#### Example

```java
public class MyAdapter<T extends RecyclerView.ViewHolder>
        extends AnimatedAdapter<T> {

    // ...

    public void setItems(@NonNull MyItem[] items) {
        this.items = items;
        animateDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    @Override
    public long getItemId(int position) {
        // Return a unique id for the item at position.
        // The id must be unique across the whole dataset.
        return items[position].getId();
    }

    @Override
    public long getItemContentHash(int position) {
        // Return a hash for the item at position.
        // The hash must change when the properties bound in this adapter change.
        return items[position].getContent().hashCode();
    }
}
```

Despite simple and convenient, everything runs on the the calling thread. For very large data sets, it can hog the UI thread and lead to dropped frames.

## `DataSetDiffer`

Calculates differences in an `Adapter`'s data set, following a call to `DataSetDiffer#diffDataSet()`. See `AsyncDataSetDiffer` for an asynchronous version more suited for larger data.

## `AsyncDataSetDiffer`

Alternative to `DataSetDiffer` that calculates the differences between two data sets in a background thread, following a call to `AsyncDataSetDiffer#diffDataSet(AsyncCallback)`.

None of the `notify*` adapter methods can be invoked between a call to `diffDataSet(AsyncCallback)` and the call to `AsyncCallback#submit()`.

#### Example

```java
public class MyAdapter<T extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<T>
        implements DataSetDiffer.Callback {
    
    // ...

    private AsyncDataSetDiffer dataSetDiffer = new AsyncDataSetDiffer(this, this);

    public setItems(@NonNull final MyItems[] items) {
        dataSetDiffer.diffDataSet(new AsyncDataSetDiffer.AsyncCallback() {
            @Override
            public int getItemCount() {
                return items.length;
            }

            @Override
            public long getItemId(int position) {
                return items[position].getId();
            }

            @Override
            public long getItemContentHash(int position) {
                return items[position].getContent().hashCode();
            }

            @Override
            public void submit() {
                this.items = items;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    @Override
    public long getItemId(int position) {
        return items[position].getId();
    }

    @Override
    public long getItemContentHash(int position) {
        return items[position].getContent().hashCode();
    }
}
```

## `WithLayerItemAnimator`

Similar to androidx's [`DefaultItemAnimator`](https://developer.android.com/reference/androidx/recyclerview/widget/DefaultItemAnimator), but all animations run in a hardware layer (see  [`ViewPropertyAnimator#withLayer()`](https://developer.android.com/reference/android/view/ViewPropertyAnimator.html#withLayer())).

## A note on `DiffUtil`

Most of the work on `AnimatedAdapter`, `DataSetDiffer` and `AsyncDataSetDiffer` was done before [`DiffUtil`](https://developer.android.com/reference/androidx/recyclerview/widget/DiffUtil) was released and rendered them mostly obsolete.

If you need to handle item moves, you might still want to use this module.

`DiffUtil` uses Eugene W. Myers's difference algorithm. This is efficient for calculating the minimum number of updates to convert one list into another, but it does not handle items that are moved. To support moves, it does a second pass, which results in a significant performance penalty. In those cases, this module should outperform it.

