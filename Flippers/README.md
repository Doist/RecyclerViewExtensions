# Flippers

[RecyclerViewExtensions](https://github.com/Doist/RecyclerViewExtensions) module providing utilities for flipping between views, such as the list, empty and loading views.

## `Flipper`

This class is not very useful by itself, but serves as base for `EmptyRecyclerFlipper` and `ProgressEmptyRecyclerFlipper`.

## `EmptyRecyclerFlipper`

Flips between a `RecyclerView`, when there are items in its `Adapter`, and a specified empty view, when there are none.

To use, simply monitor the adapter:

```java
Flipper flipper = new EmptyRecyclerFlipper(
        R.id.container, R.id.recycler, R.id.empty);
flipper.monitor(recyclerview.getAdapter());
```

## `ProgressEmptyRecyclerFlipper`

Subclass of `EmptyRecyclerFlipper`, with support for an additional state: loading. This state is not inferred automatically, and depends on calling `setLoading(boolean)` when appropriate.

## `FlipperAnimator`

Flipper animations can be customized by implementing a `FlipperAnimator` and setting it in the `Flipper` using `Flipper#setFlipperAnimator(FlipperAnimator)`.

All flippers will use a default implementation that cross-fades the views.

