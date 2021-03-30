package io.doist.recyclerviewext.flippers

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

class DelayedFlipper @JvmOverloads constructor(
    lifecycleOwner: LifecycleOwner,
    var delay: Long = DEFAULT_DURATION
) : Flipper() {
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var delayedRunnable: Runnable? = null

    init {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            private fun onDestroyed() {
                cancelPendingFlip()
                delayedRunnable = null
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
    }

    @JvmOverloads
    fun replaceDelayed(
        outView: View,
        inView: View,
        animated: Boolean,
        afterReplaceAction: (() -> Unit)? = null
    ) {
        cancelPendingFlip()
        delayedRunnable = Runnable {
            if (animated) {
                super.replace(outView, inView, afterReplaceAction)
            } else {
                super.replaceNoAnimation(outView, inView, afterReplaceAction)
            }
        }.also { runnable ->
            handler.postDelayed(runnable, delay)
        }
    }

    fun cancelPendingFlip() {
        delayedRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun replaceInternal(outView: View, inView: View, animate: Boolean) {
        cancelPendingFlip()

        // Don't animate if the outView is not visible to avoid blinks.
        val doAnimate = animate && outView.isVisible

        super.replaceInternal(outView, inView, doAnimate)
    }

    companion object {
        const val DEFAULT_DURATION: Long = 250
    }
}
