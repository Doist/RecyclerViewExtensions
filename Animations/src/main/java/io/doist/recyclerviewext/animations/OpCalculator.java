package io.doist.recyclerviewext.animations;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates the necessary changes to transform a data set to another one. Positions are determined via animation ids,
 * and non-structural changes via change ids.
 */
public class OpCalculator {
    private static List<Op> get(List<Object> fromAnimationIds, List<Object> fromChangeIds,
                                List<Object> toAnimationIds, List<Object> toChangeIds) {
        if (fromAnimationIds.size() != fromChangeIds.size() || toAnimationIds.size() != toChangeIds.size()) {
            throw new IllegalArgumentException("Animation and change ids must have the same size");
        }

        List<Op> ops = new ArrayList<>();

        ArrayList<Object> animationIds = new ArrayList<>(fromAnimationIds);
        ArrayList<Object> changeIds = new ArrayList<>(fromChangeIds);
        int toCount = toAnimationIds.size();

        // Remove up front to make positions more predictable in the second loop.
        for (int i = 0; i < animationIds.size(); i++) {
            if (Utils.indexOf(toAnimationIds, animationIds.get(i), i) == -1) {
                ops.add(new Op.Remove(i));
                animationIds.remove(i);
                changeIds.remove(i);
                i--;
            }
        }

        // Ensure lists have the needed capacity ahead of time before adding new items.
        animationIds.ensureCapacity(toCount);
        changeIds.ensureCapacity(toCount);

        // Add, move or change items based on their animation / change id.
        for (int i = 0; i < toCount; i++) {
            Object animationId = toAnimationIds.get(i);

            int oldPosition = Utils.indexOf(animationIds, animationId, i);
            if (oldPosition != -1) {
                // Item was in the previous data set, it can have moved and / or changed.

                if (oldPosition != i) {
                    // Item was at a different location, it was moved.

                    ops.add(new Op.Move(oldPosition, i));
                    animationIds.add(i, animationIds.remove(oldPosition));
                    changeIds.add(i, changeIds.remove(oldPosition));
                }

                if (!Utils.equals(toChangeIds.get(i), changeIds.get(i))) {
                    // Item was changed.

                    ops.add(new Op.Change(i));
                    changeIds.set(i, toChangeIds.get(i));
                }
            } else {
                // Item was not in the previous data set, it was added.

                ops.add(new Op.Insert(i));
                animationIds.add(i, toAnimationIds.get(i));
                changeIds.add(i, toChangeIds.get(i));
            }
        }

        return ops;
    }
}
