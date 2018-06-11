package io.doist.recyclerviewext.animations;

/**
 * Helper class to store and manage arrays of ids and content hashes as efficiently as possible, by storing them
 * contiguously in a single array in the format [id1, contenthash1, id2, contenthash2, ...].
 */
class Items {
    private long[] items;
    private int size;

    public Items() {
        this(0);
    }

    public Items(int capacity) {
        items = new long[capacity*2];
    }

    public long getId(int index) {
        return items[index * 2];
    }

    public long getContentHash(int index) {
        return items[index * 2 + 1];
    }

    public int size() {
        return size;
    }

    public void setId(int index, long id) {
        items[index * 2] = id;
    }

    public void setContentHash(int index, long contentHash) {
        items[index * 2 + 1] = contentHash;
    }

    public void add(long id, long contentHash) {
        if (size * 2 == items.length) {
            ensureCapacity(getNextSize());
        }
        items[size * 2] = id;
        items[size * 2 + 1] = contentHash;
        size++;
    }

    public void add(int index, long id, long contentHash) {
        if (size * 2 == items.length) {
            ensureCapacity(getNextSize());
        }
        System.arraycopy(items, index * 2, items, (index + 1) * 2, (size - index) * 2);
        items[index * 2] = id;
        items[index * 2 + 1] = contentHash;
        size++;
    }

    public void remove(int index) {
        remove(index, index + 1);
    }

    public void remove(int fromIndex, int toIndex) {
        System.arraycopy(items, toIndex * 2, items, fromIndex * 2, (size - toIndex) * 2);
        size -= toIndex - fromIndex;
    }

    public void clear() {
        size = 0;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity * 2 > items.length) {
            long[] items = this.items;
            this.items = new long[minimumCapacity * 2];
            System.arraycopy(items, 0, this.items, 0, size * 2);
        }
    }

    public int indexOfId(long id, int startPosition) {
        // Search back and forth until one of the ends is hit.
        for (int i = startPosition, j = 0; i >= 0 && i < size; j++, i += j % 2 == 0 ? j : -j) {
            if (id == items[i * 2]) {
                return i;
            }
        }
        if (startPosition < size / 2) {
            // Search forward if the head was hit.
            for (int i = Math.max(startPosition * 2 + 1, 0); i < size; i++) {
                if (id == items[i * 2]) {
                    return i;
                }
            }
        } else if (startPosition > size / 2) {
            // Search backward if the tail was hit.
            for (int i = Math.min(size - (size - startPosition) * 2 - 1, size - 1); i >= 0; i--) {
                if (id == items[i * 2]) {
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
