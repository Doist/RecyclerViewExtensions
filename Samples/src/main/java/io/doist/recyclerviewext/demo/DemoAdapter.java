package io.doist.recyclerviewext.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import goncalossilva.com.recyclerviewextensions.R;
import io.doist.recyclerviewext.animations.AnimatedAdapter;
import io.doist.recyclerviewext.choice_modes.Selector;
import io.doist.recyclerviewext.sticky_headers.StickyHeaders;

public class DemoAdapter extends AnimatedAdapter<BindableViewHolder> implements StickyHeaders {
    private Selector mSelector;

    private List<Object> mDataset;

    private boolean mHorizontal;

    public DemoAdapter(boolean horizontal) {
        super(true);
        mHorizontal = horizontal;
    }

    public void setDataset(List<Object> dataset) {
        mDataset = dataset;
        animateDataSetChanged();
    }

    public void setSelector(Selector selector) {
        mSelector = selector;
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
    protected Object getItemAnimationId(int position) {
        return mDataset.get(position);
    }

    @Override
    protected Object getItemChangeId(int position) {
        return mDataset.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position) instanceof String ? 0 : 1;
    }

    @Override
    public boolean isHeader(int position) {
        return mDataset.get(position) instanceof Integer;
    }

    public class DemoItemViewHolder extends BindableViewHolder implements View.OnClickListener {
        public View root;
        public TextView textView1;
        public TextView textView2;

        public DemoItemViewHolder(View root) {
            super(root);

            root.setOnClickListener(this);

            this.root = root;
            this.textView1 = (TextView) root.findViewById(android.R.id.text1);
            this.textView2 = (TextView) root.findViewById(android.R.id.text2);
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
    }

    public class DemoSectionViewHolder extends BindableViewHolder implements View.OnClickListener {
        public TextView textView;

        public DemoSectionViewHolder(View root) {
            super(root);

            root.setOnClickListener(this);
            textView = (TextView) root.findViewById(android.R.id.text1);
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
