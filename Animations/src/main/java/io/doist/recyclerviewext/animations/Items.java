package io.doist.recyclerviewext.animations;

/**
 * Helper class to store and manage arrays of ids and content hashes as efficiently as possible.
 */
class Items {
    private long[] ids;
    private int[] contentHashes;
    private int size;

    public Items() {
        this(0);
    }

    public Items(int capacity) {
        ids = new long[capacity];
        contentHashes = new int[capacity];
    }

    public long getId(int index) {
        return ids[index];
    }

    public int getContentHash(int index) {
        return contentHashes[index];
    }

    public int size() {
        return size;
    }

    public void setId(int index, long id) {
        ids[index] = id;
    }

    public void setContentHash(int index, int contentHash) {
        this.contentHashes[index] = contentHash;
    }

    public void add(long id, int contentHash) {
        if (size == ids.length) {
            ensureCapacity(getNextSize());
        }
        ids[size] = id;
        this.contentHashes[size] = contentHash;
        size++;
    }

    public void add(int index, long id, int contentHash) {
        if (size == ids.length) {
            ensureCapacity(getNextSize());
        }
        System.arraycopy(ids, index, ids, index + 1, size - index);
        System.arraycopy(contentHashes, index, contentHashes, index + 1, size - index);
        ids[index] = id;
        this.contentHashes[index] = contentHash;
        size++;
    }

    public void remove(int index) {
        remove(index, index + 1);
    }

    public void remove(int fromIndex, int toIndex) {
        System.arraycopy(ids, toIndex, ids, fromIndex, size - toIndex);
        System.arraycopy(contentHashes, toIndex, contentHashes, fromIndex, size - toIndex);
        size -= toIndex - fromIndex;
    }

    public void clear() {
        size = 0;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (ids.length < minimumCapacity) {
            long[] ids = this.ids;
            this.ids = new long[minimumCapacity];
            System.arraycopy(ids, 0, this.ids, 0, size);
            int[] contentHashes = this.contentHashes;
            this.contentHashes = new int[minimumCapacity];
            System.arraycopy(contentHashes, 0, this.contentHashes, 0, size);
        }
    }

    public int indexOfId(long id, int startPosition) {
        // Search back and forth until one of the ends is hit.
        for (int i = startPosition, j = 0; i >= 0 && i < size; j++, i += j % 2 == 0 ? j : -j) {
            if (id == ids[i]) {
                return i;
            }
        }
        if (startPosition < size / 2) {
            // Search forward if the head was hit.
            for (int i = Math.max(startPosition * 2 + 1, 0); i < size; i++) {
                if (id == ids[i]) {
                    return i;
                }
            }
        } else if (startPosition > size / 2) {
            // Search backward if the tail was hit.
            for (int i = Math.min(size - (size - startPosition) * 2 - 1, size - 1); i >= 0; i--) {
                if (id == ids[i]) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int getNextSize() {
        return size < 10 ? 10 : size + size / 2;
    }
}
