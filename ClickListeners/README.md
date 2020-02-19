# ClickListeners

[RecyclerViewExtensions](https://github.com/Doist/RecyclerViewExtensions) module providing utilities for click listening.

## `ClickableViewHolder`

`ViewHolder` implementation that gets an `OnItemClickListener` to invoke when the it is clicked. Optionally, it can also get an [`OnItemLongClickListener`](src/main/java/io/doist/recyclerviewext/click_listeners/OnItemLongClickListener.java) to invoke when long clicked.

To use, have your view holder extend from it.

## `ClickableFocusableViewHolder`

Subclass of `ClickableViewHolder` that updates the `ViewHolder`'s root view focusable state, depending on whether or not click / long click listeners are set.

