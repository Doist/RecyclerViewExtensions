# Flippers

[RecyclerViewExtensions](https://github.com/Doist/RecyclerViewExtensions) module providing utilities for flipping between views, such as the list, empty and loading views.

## `Flipper`

Flipper is responsible for the animations between states. By default uses the `DefaultFlipperAnimator` that cross-fades between Views.

## `EmptyRecyclerFlipper`

Handle flips between `RecyclerView` when there are items in its `Adapter` and a specified empty view when there are none.

### Deprecation

`EmptyRecyclerFlipper` will stop being a subclass of `Flipper`. `Flipper` used on the constructor should do all the customizations of the `EmptyRecyclerFlipper` transitions.

### Monitor usage

```java
Flipper flipper = new EmptyRecyclerFlipper(R.id.container, R.id.recycler, R.id.empty);
flipper.monitor(recyclerview.getAdapter());
```

### Custom Animations

To customize animations, use `Flipper` with a custom implementation of `FlipperAnimator`.

## `ProgressEmptyRecyclerFlipper`

Subclass of `EmptyRecyclerFlipper`, with support for an additional state: loading. This state is not inferred automatically and depends on calling `setLoading(boolean)` when appropriate.
