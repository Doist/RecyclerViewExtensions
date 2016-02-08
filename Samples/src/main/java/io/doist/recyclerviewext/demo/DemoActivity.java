package io.doist.recyclerviewext.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

import io.doist.recyclerviewext.R;
import io.doist.recyclerviewext.animations.WithLayerItemAnimator;
import io.doist.recyclerviewext.choice_modes.MultiSelector;
import io.doist.recyclerviewext.choice_modes.Selector;
import io.doist.recyclerviewext.choice_modes.SingleSelector;
import io.doist.recyclerviewext.dividers.DividerItemDecoration;
import io.doist.recyclerviewext.dragdrop.DragDropManager;
import io.doist.recyclerviewext.flippers.ProgressEmptyRecyclerFlipper;
import io.doist.recyclerviewext.sticky_headers.StickyHeadersLinearLayoutManager;

public class DemoActivity extends AppCompatActivity {
    private ViewGroup mContainer;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private DemoAdapter mAdapter;
    private ProgressEmptyRecyclerFlipper mProgressEmptyRecyclerFlipper;
    private DragDropManager mDragDropManager;

    private Selector mSelector;

    private int mDataCount = 0;
    private int mSelectorCount = 0;
    private int mLayoutCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        mContainer = (ViewGroup) findViewById(R.id.container);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLinearLayoutManager = new StickyHeadersLinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, R.drawable.divider_light, true));
        mAdapter = new DemoAdapter(false);
        mProgressEmptyRecyclerFlipper =
                new ProgressEmptyRecyclerFlipper(mContainer, R.id.recycler_view, R.id.empty, R.id.loading);
        mProgressEmptyRecyclerFlipper.monitor(mAdapter);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new WithLayerItemAnimator(true));
        mDragDropManager = new DragDropManager<>(mRecyclerView, mAdapter);
        mAdapter.setDragDropManager(mDragDropManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_load:
                mProgressEmptyRecyclerFlipper.setLoading(true);
                mContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressEmptyRecyclerFlipper.setLoading(false);
                    }
                }, 2000);
                return true;

            case R.id.action_items:
                setNextAdapterItems();
                return true;

            case R.id.action_selector:
                setNextSelector();
                return true;

            case R.id.action_layout:
                setNextLayout();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static final Object[][] ITEMS = {
            {"e", "b", "f", "f", "a", "b", "c", "d", "e"},
            {"f", "g", "h", "e"},
            {1,"a","b","c","d","e","f","g","h","i","j",2,3,"k","l",4,"m","n","o","p","q","r","s","t","u","v","w","x","y","z"},
            {2,"c","f","b"},
            {"c","e",2,"b"},
            {1,"a",2,3,"c","b","d",4,"f","e","h","j","i","g"},
            {"d","c","b","a","e","f","g","h","i","j","k","l","m","n","o"},
            {}
    };

    public void setNextAdapterItems() {
        mAdapter.setDataset(getAdapterItems());
        mDataCount++;
    }

    public List<Object> getAdapterItems() {
        return Arrays.asList(ITEMS[mDataCount % ITEMS.length]);
    }

    public void setNextSelector() {
        mSelector = getSelector();
        mAdapter.setSelector(mSelector);
        mAdapter.notifyDataSetChanged();
        mSelectorCount++;
    }

    public Selector getSelector() {
        if (mSelectorCount % 2 == 0) {
            return new SingleSelector(mRecyclerView, mAdapter);
        } else {
            return new MultiSelector(mRecyclerView, mAdapter);
        }
    }

    public void setNextLayout() {
        int orientation = mLayoutCount % 2 == 0 ? LinearLayoutManager.VERTICAL : LinearLayoutManager.HORIZONTAL;
        boolean reverse = mLayoutCount % 3 == 0;
        mAdapter = new DemoAdapter(orientation == LinearLayoutManager.HORIZONTAL);
        mAdapter.setDragDropManager(mDragDropManager);
        mAdapter.setDataset(getAdapterItems());
        if (mSelector != null) {
            mAdapter.setSelector(mSelector);
        }
        mLinearLayoutManager = new StickyHeadersLinearLayoutManager(this, orientation, reverse);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mProgressEmptyRecyclerFlipper.monitor(mAdapter);
        mRecyclerView.setAdapter(mAdapter);
        mLayoutCount++;
    }
}
