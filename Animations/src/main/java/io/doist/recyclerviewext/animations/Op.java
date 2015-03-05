package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

public abstract class Op {
    public abstract void notify(RecyclerView.Adapter adapter);

    public static class Change extends Op {
        public int positionStart;
        public int itemCount;

        public Change(int positionStart, int itemCount) {
            this.positionStart = positionStart;
            this.itemCount = itemCount;
        }

        @Override
        public void notify(RecyclerView.Adapter adapter) {
            adapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public String toString() {
            return "Change{" + "positionStart=" + positionStart + ", itemCount=" + itemCount + '}';
        }
    }

    public static class Insert extends Op {
        public int positionStart;
        public int itemCount;

        public Insert(int positionStart, int itemCount) {
            this.positionStart = positionStart;
            this.itemCount = itemCount;
        }

        @Override
        public void notify(RecyclerView.Adapter adapter) {
            adapter.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public String toString() {
            return "Insert{" + "positionStart=" + positionStart + ", itemCount=" + itemCount + '}';
        }
    }

    public static class Remove extends Op {
        public int positionStart;
        public int itemCount;

        public Remove(int positionStart, int itemCount) {
            this.positionStart = positionStart;
            this.itemCount = itemCount;
        }

        @Override
        public void notify(RecyclerView.Adapter adapter) {
            adapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public String toString() {
            return "Remove{" + "positionStart=" + positionStart + ", itemCount=" + itemCount + '}';
        }
    }

    public static class Move extends Op {
        public int fromPosition;
        public int toPosition;

        public Move(int fromPosition, int toPosition) {
            this.fromPosition = fromPosition;
            this.toPosition = toPosition;
        }

        @Override
        public void notify(RecyclerView.Adapter adapter) {
            adapter.notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public String toString() {
            return "Move{" + "fromPosition=" + fromPosition + ", toPosition=" + toPosition + '}';
        }
    }
}
