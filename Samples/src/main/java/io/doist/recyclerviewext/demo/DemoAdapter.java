package io.doist.recyclerviewext.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.doist.recyclerviewext.R;
import io.doist.recyclerviewext.animations.AnimatedAdapter;
import io.doist.recyclerviewext.choice_modes.Selector;
import io.doist.recyclerviewext.dragdrop.DragDrop;
import io.doist.recyclerviewext.dragdrop.DragDropManager;
import io.doist.recyclerviewext.sticky_headers.StickyHeaders;

public class DemoAdapter extends AnimatedAdapter<BindableViewHolder> implements StickyHeaders, DragDrop {
    private boolean mHorizontal;

    private Selector mSelector;

    private DragDropManager mDragDropManager;

    private List<Object> mDataset;

    public DemoAdapter(boolean horizontal) {
        super(true);
        mHorizontal = horizontal;
    }

    public void setDataset(List<Object> dataset) {
        mDataset = new ArrayList<>(dataset);
        animateDataSetChanged();
    }

    public void setSelector(Selector selector) {
        mSelector = selector;
    }

    public void setDragDropManager(DragDropManager dragDropManager) {
        mDragDropManager = dragDropManager;
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
    public int getItemCount() {
        return mDataset != null ? mDataset.size() : 0;
    }

    @Override
    public Object getItemAnimationId(int position) {
        return mDataset.get(position);
    }

    @Override
    public Integer getItemChangeHash(int position) {
        return mDataset.get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position) instanceof String ? 0 : 1;
    }

    @Override
    public boolean isHeader(int position) {
        return mDataset.get(position) instanceof Integer;
    }

    @Override
    public void moveItem(int from, int to) {
        mDataset.add(to, mDataset.remove(from));
    }

    public class DemoItemViewHolder extends BindableViewHolder implements View.OnClickListener,
                                                                          View.OnLongClickListener {
        public TextView textView1;
        public TextView textView2;

        public DemoItemViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            this.textView1 = (TextView) itemView.findViewById(android.R.id.text1);
            this.textView2 = (TextView) itemView.findViewById(android.R.id.text2);
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
            return mDragDropManager.start(getLayoutPosition());
        }
    }

    public class DemoSectionViewHolder extends BindableViewHolder implements View.OnClickListener {
        public TextView textView;

        public DemoSectionViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
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
