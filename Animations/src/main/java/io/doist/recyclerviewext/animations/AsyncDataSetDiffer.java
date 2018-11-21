package io.doist.recyclerviewext.animations;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adds functionality to animate differences between an adapter's data set and a new one.
 * In this variant, the differences are calculated in a background thread.
 *
 * When using this class, *never* update your data set or use any of the {@code notify*} methods between the call to
 * {@link #diffDataSet(AsyncCallback)} and the call to {@link AsyncCallback#submit()}.
 *
 * @see DataSetDiffer
 */
public class AsyncDataSetDiffer {
    private Handler handler = new Handler(Looper.getMainLooper());
    private Executor executor = new LatestTaskAsyncTaskExecutor();

    private RecyclerView.Adapter adapter;
    private DataSetDiffer dataSetDiffer;
    private int runningDiffCount = 0;

    /**
     * @param adapter  Adapter with which this data set differ is associated.
     * @param callback Callback that provides information about the items set in the adapter.
     */
    public AsyncDataSetDiffer(RecyclerView.Adapter adapter, DataSetDiffer.Callback callback) {
        if (!adapter.hasStableIds()) {
            adapter.setHasStableIds(true);
        }
        this.adapter = adapter;
        dataSetDiffer = new DataSetDiffer(adapter, callback);
    }

    /**
     * Analyzes the data set in the background using the supplied {@link AsyncCallback}.
     * When done, calls {@link AsyncCallback#submit()} to ensure the data set is updated and triggers all necessary
     * {@code notify} calls.
     *
     * @param callback Callback that provides information about the items *to be set* in the adapter.
     *                 Note the difference between this callback and the one passed in the constructor.
     */
    @UiThread
    public void diffDataSet(final AsyncCallback callback) {
        // Pause adapter monitoring to avoid double counting changes.
        if (runningDiffCount == 0) {
            dataSetDiffer.stopObservingItems();
        }
        // Ensure stop / start observing items only happens on the first / last (respectively) call to this method.
        // Note that between the original call and the runnable below runs, other calls to this method might happen.
        runningDiffCount++;

        // Diff data set in the background, apply the changes and notify in the UI thread.
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final OpDiffHandler opDiffHandler = new OpDiffHandler();
                dataSetDiffer.diffDataSet(opDiffHandler, callback);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.submit();
                        opDiffHandler.notify(adapter);

                        // Resume adapter monitoring.
                        if (runningDiffCount == 1) {
                            dataSetDiffer.startObservingItems();
                        }
                        runningDiffCount--;
                    }
                });
            }
        });
    }

    /**
     * Callback for asynchronously calculating the difference between the current data set and a new one.
     *
     * @see DataSetDiffer.Callback
     */
    public interface AsyncCallback extends DataSetDiffer.Callback {
        /**
         * Submit the data set changes, ie. set your adapter collection to the new collection.
         */
        void submit();
    }
}
