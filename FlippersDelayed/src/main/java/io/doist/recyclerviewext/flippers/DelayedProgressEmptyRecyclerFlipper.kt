package io.doist.recyclerviewext.flippers

import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView

/**
 * Similar to [ProgressEmptyRecyclerFlipper], but delays the empty state and loader entrance.
 *
 * An example of usage is a View that contains a list of local data that can take
 * a few milliseconds to load and has an entrance animation. With the delay being the same as
 * the animation, the user won't ever see the progress or the empty state view during the animation.
 */
open class DelayedProgressEmptyRecyclerFlipper constructor(
    recyclerView: RecyclerView,
    emptyView: View,
    progressView: View,
    private val delayedFlipper: DelayedFlipper
) : ProgressEmptyRecyclerFlipper(recyclerView, emptyView, progressView, delayedFlipper) {

    constructor(
        recyclerView: RecyclerView,
        emptyView: View,
        progressView: View,
        lifecycleOwner: LifecycleOwner
    ) : this(recyclerView, emptyView, progressView, DelayedFlipper(lifecycleOwner))

    constructor(
        container: ViewGroup,
        @IdRes recyclerViewId: Int,
        @IdRes emptyViewId: Int,
        @IdRes progressViewId: Int,
        delayedFlipper: DelayedFlipper
    ) : this(
        container.findViewById(recyclerViewId),
        container.findViewById(emptyViewId),
        container.findViewById(progressViewId),
        delayedFlipper
    )

    constructor(
        container: ViewGroup,
        @IdRes recyclerViewId: Int,
        @IdRes emptyViewId: Int,
        @IdRes progressViewId: Int,
        lifecycleOwner: LifecycleOwner
    ) : this(
        container.findViewById(recyclerViewId),
        container.findViewById(emptyViewId),
        container.findViewById(progressViewId),
        lifecycleOwner
    )

    override fun showEmpty(hadItems: Boolean) {
        val outView = recyclerView ?: return
        val inView = emptyView ?: return

        if (hadItems) {
            executeFlip(outView, inView, true)
        } else {
            currentView = inView
            delayedFlipper.replaceDelayed(outView, inView, true) {
                onFlipCompleted(outView, inView, true)
            }
        }
    }

    override fun setLoading(isLoading: Boolean) {
        setLoading(loading = isLoading, animate = true, canDelay = false)
    }

    override fun setLoadingNoAnimation(isLoading: Boolean) {
        setLoading(loading = isLoading, animate = true, canDelay = true)
    }

    fun setDelayedLoading(isLoading: Boolean) {
        setLoading(loading = isLoading, animate = true, canDelay = true)
    }

    fun setDelayedLoadingNoAnimation(isLoading: Boolean) {
        setLoading(loading = isLoading, animate = false, canDelay = true)
    }

    private fun showLoading(isAnimated: Boolean, canDelay: Boolean) {
        executeLoadingFlip(currentView, progressView, isAnimated, canDelay)
    }

    private fun hideLoading(isAnimated: Boolean, canDelay: Boolean) {
        executeLoadingFlip(progressView, currentView, isAnimated, canDelay)
    }

    private fun executeLoadingFlip(
        outView: View,
        inView: View,
        animate: Boolean,
        canDelay: Boolean
    ) {
        when {
            canDelay -> {
                delayedFlipper.replaceDelayed(outView, inView, animate) {
                    onLoadingFlipped(outView, inView, animate)
                }
            }
            animate -> {
                delayedFlipper.replace(outView, inView) {
                    onLoadingFlipped(outView, inView, true)
                }
            }
            else -> {
                delayedFlipper.replaceNoAnimation(outView, inView) {
                    onLoadingFlipped(outView, inView, false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean, animate: Boolean, canDelay: Boolean) {
        if (isLoadingVisible != loading) {
            isLoadingVisible = loading
            if (isLoadingVisible) {
                showLoading(animate, canDelay)
            } else {
                hideLoading(animate, canDelay)
            }
        }
    }
}
