package io.doist.recyclerviewext.dividers;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Adds dividers to {@link RecyclerView}. The default drawable is the {@link android.R.attr#listDivider} of the theme.
 *
 * If granular control is needed, for instance to hide the last divider or implement the Material Design guidelines for
 * the navigation drawer, pass in a {@link Dividers} implementation.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable mDrawable;
    private boolean mVertical;
    private Dividers mDividers;

    public DividerItemDecoration(Context context) {
        this(context, true);
    }

    public DividerItemDecoration(Context context, Dividers dividers) {
        this(context, true, dividers);
    }

    public DividerItemDecoration(Context context, boolean vertical) {
        this(context, vertical, null);
    }

    public DividerItemDecoration(Context context, boolean vertical, Dividers dividers) {
        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.listDivider});
        Drawable drawable = a.getDrawable(0);
        a.recycle();
        init(drawable, vertical, dividers);
    }

    public DividerItemDecoration(Context context, @DrawableRes int drawableId) {
        this(context, drawableId, true, null);
    }

    public DividerItemDecoration(Context context, @DrawableRes int drawableId, boolean vertical) {
        this(context, drawableId, vertical, null);
    }

    public DividerItemDecoration(Context context, @DrawableRes int drawableId, Dividers dividers) {
        this(context, drawableId, true, dividers);
    }

    public DividerItemDecoration(Context context, @DrawableRes int drawableId, boolean vertical, Dividers dividers) {
        this(context.getResources().getDrawable(drawableId), vertical, dividers);
    }

    public DividerItemDecoration(Drawable drawable) {
        this(drawable, true, null);
    }

    public DividerItemDecoration(Drawable drawable, Dividers dividers) {
        this(drawable, true, dividers);
    }

    public DividerItemDecoration(Drawable drawable, boolean vertical) {
        this(drawable, vertical, null);
    }

    public DividerItemDecoration(Drawable drawable, boolean vertical, Dividers dividers) {
        init(drawable, vertical, dividers);
    }

    private void init(Drawable drawable, boolean vertical, Dividers dividers) {
        mDrawable = drawable;
        mDrawable.setAlpha(0); // Adjusted later. Also creates the Paint in some drawables such as NinePatchDrawable.
        mVertical = vertical;
        mDividers = dividers;
    }

    public void setVertical(boolean vertical) {
        mVertical = vertical;
    }

    public void setDividers(Dividers dividers) {
        mDividers = dividers;
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        if (mVertical) {
            drawVertical(canvas, parent);
        } else {
            drawHorizontal(canvas, parent);
        }
    }

    public void drawVertical(Canvas canvas, RecyclerView parent) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (!hasDivider(parent, child)) {
                continue;
            }

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int top = child.getBottom() + params.bottomMargin + Math.round(child.getTranslationY());
            int bottom = top + mDrawable.getIntrinsicHeight();
            drawDivider(canvas, left, top, right, bottom, child.getAlpha());
        }
    }

    public void drawHorizontal(Canvas canvas, RecyclerView parent) {
        int top = parent.getPaddingTop();
        int bottom = parent.getHeight() - parent.getPaddingBottom();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (!hasDivider(parent, child)) {
                continue;
            }

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int left = child.getRight() + params.rightMargin + Math.round(child.getTranslationX());
            int right = left + mDrawable.getIntrinsicWidth();
            drawDivider(canvas, left, top, right, bottom, child.getAlpha());
        }
    }

    private void drawDivider(Canvas canvas, int left, int top, int right, int bottom, float alpha) {
        mDrawable.setAlpha((int) (alpha * 255 + 0.5f));
        mDrawable.setBounds(left, top, right, bottom);
        mDrawable.draw(canvas);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (!hasDivider(parent, view)) {
            return;
        }

        if (mVertical) {
            outRect.set(0, 0, 0, mDrawable.getIntrinsicHeight());
        } else {
            outRect.set(0, 0, mDrawable.getIntrinsicWidth(), 0);
        }
    }

    private boolean hasDivider(RecyclerView parent, View child) {
        int position = parent.getChildLayoutPosition(child);
        return position == RecyclerView.NO_POSITION || mDividers == null || mDividers.hasDivider(position);
    }
}
