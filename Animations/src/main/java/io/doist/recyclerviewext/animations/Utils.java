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
     * Similar to {@link ArrayList#indexOf(Object)}, but starts the search in a specific position before going
     * backwards, hence being optimized for collections that don't change a lot.
     */
    public static int indexOf(List<Object> list, Object object, int startPosition) {
        int size = list.size();
        for (int j = startPosition; j < size; j++) {
            if (object.equals(list.get(j))) {
                return j;
            }
        }
        for (int j = Math.min(startPosition, list.size()) - 1; j >= 0; j--) {
            if (object.equals(list.get(j))) {
                return j;
            }
        }
        return -1;
    }
}
