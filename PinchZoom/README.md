# PinchZoom

[RecyclerViewExtensions](https://github.com/Doist/RecyclerViewExtensions) module providing utilities for pinch zooming.

## `PinchZoomItemTouchListener`

A `RecyclerView.OnItemTouchListener` that invokes `PinchZoomListener#onPinchZoom(int)` whenever the gesture is detected around a specific position.

To use, add it as a touch listener to `RecyclerView`:

```java
recyclerView.addOnItemTouchListener(
        new PinchZoomItemTouchListener(context, listener));
```

