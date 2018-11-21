package io.doist.recyclerviewext.animations;

import java.util.LinkedList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Diff handler that keeps track of necessary operations to change the old data set into the new data set.
 */
class OpDiffHandler implements DiffHandler {
    private List<Op> ops = new LinkedList<>();

    public List<Op> getOps() {
        return ops;
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        ops.add(new Op.Change(positionStart, itemCount));
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        ops.add(new Op.Insert(positionStart, itemCount));
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        ops.add(new Op.Remove(positionStart, itemCount));
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        ops.add(new Op.Move(fromPosition, toPosition));
    }

    public void notify(RecyclerView.Adapter adapter) {
        for (Op op : ops) {
            op.notify(adapter);
        }
        ops.clear();
    }

    public abstract static class Op {
        public abstract void notify(RecyclerView.Adapter adapter);

        static class Change extends Op {
            private int positionStart;
            private int itemCount;

            Change(int positionStart, int itemCount) {
                this.positionStart = positionStart;
                this.itemCount = itemCount;
            }

            @Override
            public void notify(RecyclerView.Adapter adapter) {
                adapter.notifyItemRangeChanged(positionStart, itemCount);
            }
        }

        static class Insert extends Op {
            private int positionStart;
            private int itemCount;

            Insert(int positionStart, int itemCount) {
                this.positionStart = positionStart;
                this.itemCount = itemCount;
            }

            @Override
            public void notify(RecyclerView.Adapter adapter) {
                adapter.notifyItemRangeInserted(positionStart, itemCount);
            }
        }

        static class Remove extends Op {
            private int positionStart;
            private int itemCount;

            Remove(int positionStart, int itemCount) {
                this.positionStart = positionStart;
                this.itemCount = itemCount;
            }

            @Override
            public void notify(RecyclerView.Adapter adapter) {
                adapter.notifyItemRangeRemoved(positionStart, itemCount);
            }
        }

        static class Move extends Op {
            private int fromPosition;
            private int toPosition;

            Move(int fromPosition, int toPosition) {
                this.fromPosition = fromPosition;
                this.toPosition = toPosition;
            }

            @Override
            public void notify(RecyclerView.Adapter adapter) {
                adapter.notifyItemMoved(fromPosition, toPosition);
            }
        }
    }
}
