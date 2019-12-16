package io.doist.recyclerviewext.dragdrop;

import android.content.ClipData;
import android.os.SystemClock;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * {@link View.OnDragListener} for {@link RecyclerView} that maps calls to a {@link RecyclerView.OnItemTouchListener}.
 *
 * {@link RecyclerView.OnItemTouchListener} will typically be {@link DragDropHelper}, so that it tracks drag events
 * started via the system APIs (e.g. {@link View#startDragAndDrop(ClipData, View.DragShadowBuilder, Object, int)}.
 */
public class DragDropTouchDragListener implements View.OnDragListener {
    private RecyclerView.OnItemTouchListener onItemTouchListener;
    private boolean intercept;
    private long startTime;

    public DragDropTouchDragListener(RecyclerView.OnItemTouchListener onItemTouchListener) {
        this.onItemTouchListener = onItemTouchListener;
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                intercept = false;
                startTime = SystemClock.uptimeMillis();
                dispatchTouchEvent(view, MotionEvent.ACTION_DOWN, event.getX(), event.getY());
                // Return true to receive further events.
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                dispatchTouchEvent(view, MotionEvent.ACTION_MOVE, event.getX(), event.getY());
                return false;

            case DragEvent.ACTION_DRAG_ENDED:
                dispatchTouchEvent(view, MotionEvent.ACTION_UP, event.getX(), event.getY());
                return false;

            default:
                return false;
        }
    }

    private void dispatchTouchEvent(View view, int action, float x, float y) {
        if (!(view instanceof RecyclerView)) {
            throw new IllegalStateException("DragDropTouchDragListener must be set on a RecyclerView");
        }
        MotionEvent motionEvent = MotionEvent.obtain(startTime, SystemClock.uptimeMillis(), action, x, y, 0);
        if (this.intercept) {
            onItemTouchListener.onTouchEvent((RecyclerView) view, motionEvent);
        } else {
            this.intercept = onItemTouchListener.onInterceptTouchEvent((RecyclerView) view, motionEvent);
        }
        motionEvent.recycle();
    }
}
