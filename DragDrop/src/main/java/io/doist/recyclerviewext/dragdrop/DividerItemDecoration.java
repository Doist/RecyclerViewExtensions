package io.doist.recyclerviewext.dragdrop;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDivider;
    private boolean mVertical;
    private boolean mShowLastDivider = false;

    public DividerItemDecoration(Context context) {
        this(context, true);
    }

    public DividerItemDecoration(Context context, boolean vertical) {
        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.listDivider});
        mDivider = a.getDrawable(0);
        a.recycle();
        mVertical = vertical;
    }

    public DividerItemDecoration(Context context, @DrawableRes int drawableId) {
        this(context, drawableId, true);
    }

    public DividerItemDecoration(Context context, @DrawableRes int drawableId, boolean vertical) {
        mDivider = context.getResources().getDrawable(drawableId);
        mVertical = vertical;
    }

    public DividerItemDecoration(Drawable divider) {
        this(divider, true);
    }

    public DividerItemDecoration(Drawable divider, boolean vertical) {
        mDivider = divider;
        mVertical = vertical;
    }

    public void setVertical(boolean vertical) {
        mVertical = vertical;
    }

    public void setShowLastDivider(boolean showLastDivider) {
        mShowLastDivider = showLastDivider;
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        if (mVertical) {
            drawVertical(canvas, parent, state);
        } else {
            drawHorizontal(canvas, parent, state);
        }
    }

    public void drawVertical(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (i == childCount - 1 && !mShowLastDivider && isLastItem(parent, state, child)) {
                break;
            }

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int top = child.getBottom() + params.bottomMargin + Math.round(child.getTranslationY());
            int bottom = top + mDivider.getIntrinsicHeight();
            drawDivider(canvas, left, top, right, bottom, child.getAlpha());
        }
    }

    public void drawHorizontal(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        int top = parent.getPaddingTop();
        int bottom = parent.getHeight() - parent.getPaddingBottom();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (i == childCount - 1 && !mShowLastDivider && isLastItem(parent, state, child)) {
                break;
            }

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int left = child.getRight() + params.rightMargin + Math.round(child.getTranslationX());
            int right = left + mDivider.getIntrinsicWidth();
            drawDivider(canvas, left, top, right, bottom, child.getAlpha());
        }
    }

    private void drawDivider(Canvas canvas, int left, int top, int right, int bottom, float alpha) {
        mDivider.setAlpha((int) (alpha * 255 + 0.5f));
        mDivider.setBounds(left, top, right, bottom);
        mDivider.draw(canvas);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (!mShowLastDivider && isLastItem(parent, state, view)) {
            return;
        }

        if (mVertical) {
            outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        } else {
            outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
        }
    }

    private boolean isLastItem(RecyclerView parent, RecyclerView.State state, View view) {
        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        return holder != null && holder.getLayoutPosition() == state.getItemCount() - 1;
    }
}
