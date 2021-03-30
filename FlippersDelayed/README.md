# Delayed Flippers

Extends [Flippers](../Flippers) features by providing delayed transitions between states. 

Delayed Flippers can come in handy, for example, when showing a new screen and the loading takes less time than the screen takes to appear. With a slight delay in the loading state, the user will not see the loading before viewing the content.

## Setup

`FlippersDelayed` depend on `Flippers`. It must have `Flippers` module configured on the project.

### Example

**settings.gradle**

```
include ':Flippers', ':FlippersDelayed'
```

**build.gradle**

```
dependencies {
	...
	implementation project(':Flippers')
	implementation project(':FlippersDelayed')
	...
}
```

## `DelayedFlipper`

`DelayedFlipper` extends the capability of [`Flipper`](../Flippers/src/main/java/io/doist/recyclerviewext/flippers/Flipper.java) by adding the option of starting a transition of state delayed.

By default uses the DefaultFlipperAnimator that cross-fades between Views, but it can be customized.

The default delay is `250ms`. To customize it, set the `delay` value in the initialization call `DelayedFlipper(LifecycleOwner, Long)`.

## `DelayedEmptyRecyclerFlipper`

It is almost the same as [`EmptyRecyclerFlipper`](../Flippers/src/main/java/io/doist/recyclerviewext/flippers/EmptyRecyclerFlipper.java). 

The difference is:

- Initial empty state appearance is delayed.

## `DelayedProgressEmptyRecyclerFlipper`

It is almost the same as [`ProgressEmptyRecyclerFlipper`](../Flippers/src/main/java/io/doist/recyclerviewext/flippers/ProgressEmptyRecyclerFlipper.java). 

The differences are:

- Initial empty state appearance is delayed.
- The loading state can be delayed with `setDelayedLoading(Boolean)`.
