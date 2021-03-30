package io.doist.recyclerviewext.flippers

import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView

/**
 * Similar to [EmptyRecyclerFlipper], but delays the empty state appearance.
 *
 * An example of usage is a View that contains a list of local data that can take
 * a few milliseconds to load and has an entrance animation. With the delay being the same as
 * the animation, the user won't ever see the empty state view during the animation.
 */
open class DelayedEmptyRecyclerFlipper(
    recyclerView: RecyclerView,
    emptyView: View,
    private val delayedFlipper: DelayedFlipper
) : EmptyRecyclerFlipper(recyclerView, emptyView, delayedFlipper) {

    constructor(
        recyclerView: RecyclerView,
        emptyView: View,
        lifecycleOwner: LifecycleOwner
    ) : this(recyclerView, emptyView, DelayedFlipper(lifecycleOwner))

    constructor(
        container: ViewGroup,
        @IdRes recyclerViewId: Int,
        @IdRes emptyViewId: Int,
        delayedFlipper: DelayedFlipper
    ) : this(
        container.findViewById(recyclerViewId),
        container.findViewById(emptyViewId),
        delayedFlipper
    )

    constructor(
        container: ViewGroup,
        @IdRes recyclerViewId: Int,
        @IdRes emptyViewId: Int,
        lifecycleOwner: LifecycleOwner
    ) : this(
        container.findViewById(recyclerViewId),
        container.findViewById(emptyViewId),
        lifecycleOwner
    )

    override fun showContent(animate: Boolean) {
        executeFlip(emptyView, recyclerView, animate)
    }

    override fun showEmpty(hadItems: Boolean) {
        val outView = recyclerView ?: return
        val inView = emptyView ?: return

        if (hadItems) {
            executeFlip(outView, inView, false)
        } else {
            delayedFlipper.replaceDelayed(outView, inView, true) {
                onFlipCompleted(outView, inView, false)
            }
        }
    }
}
