package io.doist.recyclerviewext.demo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.doist.recyclerviewext.R;
import io.doist.recyclerviewext.animations.AnimatedAdapter;
import io.doist.recyclerviewext.choice_modes.Selector;
import io.doist.recyclerviewext.dragdrop.DragDropHelper;
import io.doist.recyclerviewext.sticky_headers.StickyHeaders;

public class DemoAdapter extends AnimatedAdapter<BindableViewHolder>
        implements StickyHeaders, DragDropHelper.Callback {
    private boolean mHorizontal;

    private Selector mSelector;

    private DragDropHelper mDragDropHelper;

    private List<Object> mDataset;

    DemoAdapter(boolean horizontal) {
        super();
        mHorizontal = horizontal;
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

    @Override
    public BindableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            return new DemoItemViewHolder(
                    inflater.inflate(mHorizontal ? R.layout.item_horizontal : R.layout.item, parent, false));
        } else {
            return new DemoSectionViewHolder(
                    inflater.inflate(mHorizontal ? R.layout.section_horizontal : R.layout.section, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(BindableViewHolder holder, int position) {
        if (mSelector != null) {
            mSelector.bind(holder);
        }
        holder.bind(mDataset.get(position));
    }

    @Override
    public long getItemId(int position) {
        return mDataset.get(position).hashCode();
    }

    @Override
    public int getItemChangeHash(int position) {
        return 0;
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
    public void onDragStarted(final RecyclerView.ViewHolder holder) {
        holder.itemView.setBackgroundColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.itemView.animate().translationZ(8f).setDuration(200L).setListener(new AnimatorListenerAdapter() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onAnimationEnd(Animator animation) {
                    holder.itemView.setTranslationZ(8f);
                }
            });
        }
    }

    @Override
    public boolean canSwap(RecyclerView.ViewHolder holder, RecyclerView.ViewHolder target) {
        return true; //holder.getClass() == target.getClass();
    }

    @Override
    public void onSwap(RecyclerView.ViewHolder holder, RecyclerView.ViewHolder target) {
        int from = holder.getAdapterPosition();
        int to = target.getAdapterPosition();
        mDataset.add(to, mDataset.remove(from));
        notifyItemMoved(from, to);
    }

    @Override
    public void onDragStopped(final RecyclerView.ViewHolder holder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.itemView.animate().translationZ(0f).setDuration(200L).setListener(new AnimatorListenerAdapter() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onAnimationEnd(Animator animation) {
                    holder.itemView.setTranslationZ(0f);
                }
            });
        }
    }

    public class DemoItemViewHolder extends BindableViewHolder implements View.OnClickListener,
                                                                          View.OnLongClickListener {
        TextView textView1;
        TextView textView2;

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
            mDragDropHelper.start(DemoItemViewHolder.this);
            return true;
        }
    }

    public class DemoSectionViewHolder extends BindableViewHolder implements View.OnClickListener {
        TextView textView;

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
