# Selectors

[RecyclerViewExtensions](https://github.com/Doist/RecyclerViewExtensions) module providing utilities for selections, akin to `ListView`'s choice modes.

## `SingleSelector`

Tracks a single selection, similar to `ListView#CHOICE_MODE_SINGLE`.

The state can be updated using `toggleSelected(int)` or `setSelected(int, boolean)`, typically inside a click listener.

`notifyItemChanged(int, Object)` is invoked automatically with the `PAYLOAD_SELECT` payload whenever the selection changes. Callbacks are also provided, via `OnSelectionChangedListener`.

`Selector` also provides a `bind(ViewHolder, boolean)` method, which will set/unset the activated state depending on whether an item is selected or not (see [`android.R.attr#state_activated`](https://developer.android.com/reference/android/R.attr.html#state_activated)).

#### Example

```java
public class MyAdapter<T extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<T>
        implements Selector.OnSelectionChangedListener {
    
    // ...

    private Selector selector;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        selector = new SingleSelector(recyclerView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull T holder, int position) {
        selector.bind(holder, true);
    }

    @Override
    public void onSelectionChanged(long[] selectedIds, long[] previousSelectedIds) {
        // The selection has changed.
    }

    private class MyViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        
        public MyViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            selector.toggleSelected(getItemId());
        }
    }
}
```

## `MultiSelector`

Tracks multiple selections, similar to `ListView#CHOICE_MODE_MULTIPLE`.

Other than this, the behavior and workflow is the same as with `SingleSelector`.
