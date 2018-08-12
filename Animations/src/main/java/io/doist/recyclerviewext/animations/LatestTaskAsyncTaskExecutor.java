package io.doist.recyclerviewext.animations;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Executor that only keeps track of the latest task, reusing {@link AsyncTask#THREAD_POOL_EXECUTOR} internally.
 */
class LatestTaskAsyncTaskExecutor implements Executor {
    private Runnable next;
    private Runnable active;

    public synchronized void execute(@NonNull final Runnable r) {
        next = new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        };
        if (active == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        active = next;
        next = null;
        if (active != null) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(active);
        }
    }
}

