package io.doist.recyclerviewext.animations;

import java.util.ArrayList;
import java.util.List;

class Utils {
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a != null) {
            return a.equals(b);
        } else {
            return false;
        }
    }

    /**
     * Similar to {@link ArrayList#indexOf(Object)}, but starts the search in a specific position going back and forth,
     * hence being optimized for collections that don't change a lot.
     */
    public static int indexOf(List<Object> list, Object object, int startPosition) {
        int size = list.size();
        // Search back and forth until one of the ends is hit.
        for (int i = startPosition, j = 0; i >= 0 && i < size; j++, i += j % 2 == 0 ? j : -j) {
            if (equals(object, list.get(i))) {
                return i;
            }
        }
        if (startPosition < size / 2) {
            // Search forward if the head was hit.
            for (int i = Math.max(startPosition * 2 + 1, 0); i < size; i++) {
                if (equals(object, list.get(i))) {
                    return i;
                }
            }
        } else if (startPosition > size / 2) {
            // Search backward if the tail was hit.
            for (int i = Math.min(size - (size - startPosition) * 2 - 1, size - 1); i >= 0; i--) {
                if (equals(object, list.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }
}
