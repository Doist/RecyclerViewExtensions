package io.doist.recyclerviewext.animations;

import android.support.v7.widget.RecyclerView;

public abstract class Op {
    public abstract void notify(RecyclerView.Adapter adapter);

    public static class Change extends Op {
        public int position;

        public Change(int position) {
            this.position = position;
        }

        @Override
        public void notify(RecyclerView.Adapter adapter) {
            adapter.notifyItemChanged(position);
        }
    }

    public static class Insert extends Op {
        public int position;

        public Insert(int position) {
            this.position = position;
        }

        @Override
        public void notify(RecyclerView.Adapter adapter) {
            adapter.notifyItemInserted(position);
        }
    }

    public static class Remove extends Op {
        public int position;

        public Remove(int position) {
            this.position = position;
        }

        @Override
        public void notify(RecyclerView.Adapter adapter) {
            adapter.notifyItemRemoved(position);
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
    }
}
