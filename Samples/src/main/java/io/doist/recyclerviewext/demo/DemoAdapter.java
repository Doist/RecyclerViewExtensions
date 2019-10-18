package io.doist.recyclerviewext.demo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.doist.recyclerviewext.R;
import io.doist.recyclerviewext.animations.AnimatedAdapter;
import io.doist.recyclerviewext.choice_modes.Selector;
import io.doist.recyclerviewext.dragdrop.DragDropHelper;
import io.doist.recyclerviewext.sticky_headers.StickyHeaders;

public class DemoAdapter extends AnimatedAdapter<BindableViewHolder>
        implements StickyHeaders, DragDropHelper.Callback {
    @RecyclerView.Orientation
    private final int mOrientation;

    private Selector mSelector;

    private DragDropHelper mDragDropHelper;

    private List<Object> mDataset;

    DemoAdapter(int orientation) {
        super();
        mOrientation = orientation;
    }

    void setDataset(List<Object> dataset) {
        mDataset = new ArrayList<>(dataset);
        animateDataSetChanged();
    }

    void setSelector(Selector selector) {
        mSelector = selector;
    }

    void setDragDropHelper(DragDropHelper dragDropHelper) {
        mDragDropHelper = dragDropHelper;
    }

    @NonNull
    @Override
    public BindableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new DemoItemViewHolder(inflater.inflate(
                    mOrientation == RecyclerView.VERTICAL ?
                    R.layout.item : R.layout.item_horizontal, parent, false));
        } else {
            return new DemoSectionViewHolder(inflater.inflate(
                    mOrientation == RecyclerView.VERTICAL ?
                    R.layout.section : R.layout.section_horizontal, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BindableViewHolder holder, int position) {
        if (mSelector != null) {
            mSelector.bind(holder, true);
        }
        holder.bind(mDataset.get(position));
    }

    @Override
    public long getItemId(int position) {
        return mDataset.get(position).hashCode();
    }

    @Override
    public long getItemContentHash(int position) {
        return 0L;
    }

    @Override
    public int getItemCount() {
        return mDataset != null ? mDataset.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position) instanceof String ? 0 : 1;
    }

    @Override
    public boolean isStickyHeader(int position) {
        return mDataset.get(position) instanceof Integer;
    }

    @Override
    public void onDragStarted(@NonNull final RecyclerView.ViewHolder holder, boolean create) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (create) {
                holder.itemView.animate().translationZ(8f).setDuration(200L).setListener(new AnimatorListenerAdapter() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        holder.itemView.setTranslationZ(8f);
                    }
                });
            } else {
                holder.itemView.setTranslationZ(8f);
            }
        }
    }

    @Override
    public boolean onDragMoved(@NonNull RecyclerView.ViewHolder holder, int x, int y) {
        Rect rect = new Rect();
        ((View) holder.itemView.getParent()).getDrawingRect(rect);
        return rect.contains(x, y);
    }

    @Override
    public int onDragTo(@NonNull RecyclerView.ViewHolder holder, int to) {
        int from = holder.getAdapterPosition();
        mDataset.add(to, mDataset.remove(from));
        notifyItemMoved(from, to);
        return to;
    }

    @Override
    public void onDragStopped(@NonNull final RecyclerView.ViewHolder holder, boolean destroy) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (destroy) {
                holder.itemView.animate().translationZ(0f).setDuration(200L).setListener(new AnimatorListenerAdapter() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        holder.itemView.setTranslationZ(0f);
                    }
                });
            } else {
                holder.itemView.setTranslationZ(0f);
            }
        }
    }

    public class DemoItemViewHolder extends BindableViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        final TextView textView1;
        final TextView textView2;

        DemoItemViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            this.textView1 = itemView.findViewById(android.R.id.text1);
            this.textView2 = itemView.findViewById(android.R.id.text2);
        }

        @Override
        public void bind(Object object) {
            textView1.setText(String.valueOf(object));
        }

        @Override
        public void onClick(View v) {
            if (mSelector != null) {
                mSelector.toggleSelected(getItemId());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            mDragDropHelper.start(getAdapterPosition());
            return true;
        }
    }

    public class DemoSectionViewHolder extends BindableViewHolder implements View.OnClickListener {
        final TextView textView;

        DemoSectionViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            textView = itemView.findViewById(android.R.id.text1);
        }

        @Override
        public void bind(Object object) {
            textView.setText(String.valueOf(object));
        }

        @Override
        public void onClick(View itemView) {
            textView.animate().rotationBy(360).setDuration(500).start();
        }
    }
}
