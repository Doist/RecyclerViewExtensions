# DragDrop

[RecyclerViewExtensions](https://github.com/Doist/RecyclerViewExtensions) module providing utilities for drag & drop.

## `DragDropHelper`

Supports drag & drop for `RecyclerView` items, provided it is using a `LinearLayoutManager`.

To use, call `attach(RecyclerView, Callback)`  and provide a `Callback` to handle the lifecycle of the drag. Afterwards, call `start(int)` for any position, and `DragDropHelper` will monitor touch events to handle the drag until it ends.

#### Example

```java
public class MyAdapter<T extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<T>
        implements DragDropHelper.Callback {

    // ...

    private DragDropHelper dragDropHelper = new DragDropHelper();

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        dragDropHelper.attach(recyclerView, this);
    }

    @Override
    public void onDragStarted(@NonNull RecyclerView.ViewHolder holder, boolean create) {
        // Setup the holder.
    }

    @Override
    public void onDragMoved(@NonNull RecyclerView.ViewHolder holder, int x, int y) {
        // Handle drag coordinates.
    }

    @Override
    public int onDragTo(@NonNull RecyclerView.ViewHolder holder, int to) {
        int from = holder.getAdapterPosition();
        dataset.add(to, dataset.remove(from));
        notifyItemMoved(from, to);
        return to;
    }

    @Override
    public void onDragStopped(@NonNull RecyclerView.ViewHolder holder, boolean destroy) {
        // Undo holder setup.
    }
}
```

Depending on the use case, different flags can be tweaked in `DragDropHelper` to ensure it behaves as desired.

## `DragDropTouchDragListener`

`View.OnDragListener` that maps calls to a `RecyclerView.OnItemTouchListener`.

When the `RecyclerView.OnItemTouchListener` is `DragDropHelper`, it enables its use with Android's native drag & drop APIs, reusing its ability to handle view swapping, scrolling, etc.

