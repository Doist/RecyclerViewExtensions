package io.doist.recyclerviewext.animations;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataSetDifferTest {
    private TestDataSetDiffer differ;
    private TestCallback callback;

    @Before
    public void setUp() throws Exception {
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new FakeAdapter();
        // Plain JVM unit tests run against a mockable android.jar whose constructor bodies are
        // stripped, so the mObservers field the adapter's observable inherits from
        // android.database.Observable is never initialized.
        // Patch it before the differ registers its observer.
        Field observableField = RecyclerView.Adapter.class.getDeclaredField("mObservable");
        observableField.setAccessible(true);
        Object observable = observableField.get(adapter);
        Field observersField = android.database.Observable.class.getDeclaredField("mObservers");
        observersField.setAccessible(true);
        observersField.set(observable, new ArrayList<>());

        differ = new TestDataSetDiffer(adapter, callback = new TestCallback());
    }

    @Test
    public void noChange() {
        List<String> ops = diff(range(1, 10), range(1, 10), null);
        assertEquals(List.of(), ops);
    }

    @Test
    public void insert() {
        List<String> ops = diff(new long[]{1, 3}, new long[]{1, 2, 3}, null);
        assertEquals(List.of("I(1,1)"), ops);
    }

    @Test
    public void remove() {
        List<String> ops = diff(new long[]{1, 2, 3}, new long[]{1, 3}, null);
        assertEquals(List.of("R(1,1)"), ops);
    }

    @Test
    public void change() {
        List<String> ops = diff(new long[]{1, 2, 3}, new long[]{1, 2, 3}, new long[]{10, 20, 30},
                                new long[]{10, 25, 30});
        assertEquals(List.of("C(1,1)"), ops);
    }

    @Test
    public void swap() {
        List<String> ops = diff(new long[]{1, 2, 3}, new long[]{2, 1, 3}, null);
        assertEquals(List.of("M(1,0)"), ops);
    }

    @Test
    public void shortShift_notifiedAsUnitMoves() {
        // Run of 3 shifted items is below MIN_ROTATION_LENGTH, so each shifted item keeps
        // its move op.
        List<String> ops = diff(new long[]{1, 2, 3, 4, 5}, new long[]{1, 3, 4, 5, 2}, null);
        assertEquals(List.of("M(2,1)", "M(3,2)", "M(4,3)"), ops);
    }

    @Test
    public void longRotation_notifiedAsSingleMove() {
        // Item 2 moves from position 1 to the end while the 20 items in between shift one position
        // forward, e.g. a recurring task rescheduled months ahead in a day-sectioned list.
        long[] newIds = concat(range(1, 1), range(3, 22), new long[]{2});
        List<String> ops = diff(range(1, 22), newIds, null);
        assertEquals(List.of("M(1,21)"), ops);
    }

    @Test
    public void longRotationWithContentChange_notifiedAsMoveAndChange() {
        long[] newIds = concat(range(1, 1), range(3, 22), new long[]{2});
        long[] newContentHashes = newIds.clone();
        newContentHashes[21] = 999;
        List<String> ops = diff(range(1, 22), newIds, null, newContentHashes);
        assertEquals(List.of("M(1,21)", "C(21,1)"), ops);
    }

    @Test
    public void longRotationInterruptedByNewItem_notifiedAsUnitMoves() {
        // Item 99 is inserted right before the displaced item lands, so the rotation doesn't hold
        // and the shift keeps the per-item move ops.
        long[] newIds = concat(range(1, 1), range(3, 22), new long[]{99, 2});
        List<String> ops = diff(range(1, 22), newIds, null);

        List<String> expected = new ArrayList<>();
        for (int i = 2; i <= 21; i++) {
            expected.add("M(" + i + "," + (i - 1) + ")");
        }
        expected.add("I(21,1)");
        assertEquals(expected, ops);
    }

    @Test
    public void longRotationInterruptedByNewItem_probesRotationOnce() {
        int probesBefore = differ.rotationProbeCount;
        long[] newIds = concat(range(1, 1), range(3, 22), new long[]{99, 2});
        List<String> ops = diff(range(1, 22), newIds, null);

        List<String> expected = new ArrayList<>();
        for (int i = 2; i <= 21; i++) {
            expected.add("M(" + i + "," + (i - 1) + ")");
        }
        expected.add("I(21,1)");
        assertEquals(expected, ops);
        assertEquals(1, differ.rotationProbeCount - probesBefore);
    }

    @Test
    public void longRotationInterruptedNearStart_resumesAfterInsertion() {
        int probesBefore = differ.rotationProbeCount;
        long[] newIds = concat(range(1, 1), new long[]{3, 99}, range(4, 22), new long[]{2});
        List<String> ops = diff(range(1, 22), newIds, null);

        assertEquals(List.of("M(2,1)", "I(2,1)", "M(3,22)"), ops);
        assertEquals(2, differ.rotationProbeCount - probesBefore);
    }

    @Test
    public void earlyRotationBlocker_stopsAtFirstBlocker() {
        callback.ids = range(1, 22);
        differ.diffDataSet(new RecordingDiffHandler(), callback);

        CountingItems newItems = new CountingItems(
                concat(range(1, 1), new long[]{3, 99}, range(4, 22), new long[]{2}));
        differ.findRotation(newItems, 1);

        assertEquals(
                "Rotation probe searched for the displaced item before reaching the blocker",
                0,
                newItems.indexOfIdCalls);
        assertTrue(
                "Rotation probe read past the blocker at position 2: " + newItems.lastReadPosition,
                newItems.lastReadPosition <= 2);
    }

    @Test
    public void longRotationInterruptedInMiddle_notifiedAsUnitMoves() {
        // Item 2 appears to travel far, but item 99 breaks the shifted sequence. The displaced item
        // still lands before the list end, so only the validation loop can reject the rotation.
        long[] newIds = concat(
                range(1, 1),
                range(3, 17),
                new long[]{99},
                range(18, 29),
                new long[]{2, 30});
        List<String> ops = diff(range(1, 30), newIds, null);

        List<String> expected = new ArrayList<>();
        for (int i = 2; i <= 16; i++) {
            expected.add("M(" + i + "," + (i - 1) + ")");
        }
        expected.add("I(16,1)");
        for (int i = 18; i <= 29; i++) {
            expected.add("M(" + i + "," + (i - 1) + ")");
        }
        assertEquals(expected, ops);
    }

    @Test
    public void farMoveBack_notifiedAsSingleMove() {
        // Item 25 moves from the end to position 2, e.g. an undo restoring a task to today.
        long[] newIds = concat(range(1, 2), new long[]{25}, range(3, 24));
        List<String> ops = diff(range(1, 25), newIds, null);
        assertEquals(List.of("M(24,2)"), ops);
    }

    private List<String> diff(long[] oldIds, long[] newIds, long[] oldContentHashes) {
        return diff(oldIds, newIds, oldContentHashes, null);
    }

    private List<String> diff(
            long[] oldIds,
            long[] newIds,
            long[] oldContentHashes,
            long[] newContentHashes) {
        callback.ids = oldIds;
        callback.contentHashes = oldContentHashes;
        // First diff populates the differ with the old data set; its ops are irrelevant.
        differ.diffDataSet(new RecordingDiffHandler(), callback);

        callback.ids = newIds;
        callback.contentHashes = newContentHashes;
        RecordingDiffHandler diffHandler = new RecordingDiffHandler();
        differ.diffDataSet(diffHandler, callback);
        return diffHandler.ops;
    }

    private static long[] range(int fromInclusive, int toInclusive) {
        long[] ids = new long[toInclusive - fromInclusive + 1];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = fromInclusive + i;
        }
        return ids;
    }

    private static long[] concat(long[]... parts) {
        int length = 0;
        for (long[] part : parts) {
            length += part.length;
        }
        long[] ids = new long[length];
        int offset = 0;
        for (long[] part : parts) {
            System.arraycopy(part, 0, ids, offset, part.length);
            offset += part.length;
        }
        return ids;
    }

    private static class CountingItems extends Items {
        private int indexOfIdCalls;
        private int lastReadPosition = -1;

        private CountingItems(long[] ids) {
            for (long id : ids) {
                add(id, id);
            }
        }

        @Override
        public long getId(int index) {
            lastReadPosition = Math.max(lastReadPosition, index);
            return super.getId(index);
        }

        @Override
        public int indexOfId(long id, int startPosition) {
            indexOfIdCalls++;
            return super.indexOfId(id, startPosition);
        }
    }

    private static class TestDataSetDiffer extends DataSetDiffer {
        private int rotationProbeCount;

        private TestDataSetDiffer(RecyclerView.Adapter adapter, Callback callback) {
            super(adapter, callback);
        }

        @Override
        DataSetDiffer.RotationResult findRotation(Items newItems, int start) {
            rotationProbeCount++;
            return super.findRotation(newItems, start);
        }
    }

    private static class FakeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(
                @NonNull android.view.ViewGroup parent,
                int viewType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }

    private static class TestCallback implements DataSetDiffer.Callback {
        private long[] ids;
        private long[] contentHashes;

        @Override
        public int getItemCount() {
            return ids.length;
        }

        @Override
        public long getItemId(int position) {
            return ids[position];
        }

        @Override
        public long getItemContentHash(int position) {
            return contentHashes == null ? ids[position] : contentHashes[position];
        }
    }

    private static class RecordingDiffHandler implements DiffHandler {
        private final List<String> ops = new ArrayList<>();

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            ops.add("C(" + positionStart + "," + itemCount + ")");
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            ops.add("I(" + positionStart + "," + itemCount + ")");
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            ops.add("R(" + positionStart + "," + itemCount + ")");
        }

        @Override
        public void onItemMoved(int fromPosition, int toPosition) {
            ops.add("M(" + fromPosition + "," + toPosition + ")");
        }
    }
}
