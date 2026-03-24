package com.binpacker.lib.optimizer.mutators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.optimizer.Solution;

public class SpaceMutation {

    public static List<Integer> modify(Random random, Solution currentSequence, Solution second, Bin bin,
            List<Box> originalBoxes) {
        List<Integer> mutatedOrder = new ArrayList<>(currentSequence.order);

        float maxEmptySpace = -1f;
        Box targetBox = null;

        // Find the box with the most free space in positive X, Y, Z directions from its
        // center
        // Ignore the very last bin if there is more than 1, as it is almost guaranteed
        // to be partially empty
        int numBinsToCheck = currentSequence.solved.size();
        if (numBinsToCheck > 1) {
            numBinsToCheck--;
        }

        for (int b = 0; b < numBinsToCheck; b++) {
            List<Box> binBoxes = currentSequence.solved.get(b);
            for (Box box : binBoxes) {
                float cx = box.position.x + box.size.x / 2.0f;
                float cy = box.position.y + box.size.y / 2.0f;
                float cz = box.position.z + box.size.z / 2.0f;

                float xRight = box.position.x + box.size.x;
                float yTop = box.position.y + box.size.y;
                float zTop = box.position.z + box.size.z;

                // Track lowest blockage in each direction
                float nextX = bin.w;
                float nextY = bin.h;
                float nextZ = bin.d; // Fallback to bin boundaries

                for (Box other : binBoxes) {
                    if (other == box)
                        continue;

                    // Check +X ray
                    if (other.position.x >= xRight) {
                        if (cy >= other.position.y && cy <= other.position.y + other.size.y &&
                                cz >= other.position.z && cz <= other.position.z + other.size.z) {
                            if (other.position.x < nextX)
                                nextX = other.position.x;
                        }
                    }

                    // Check +Y ray
                    if (other.position.y >= yTop) {
                        if (cx >= other.position.x && cx <= other.position.x + other.size.x &&
                                cz >= other.position.z && cz <= other.position.z + other.size.z) {
                            if (other.position.y < nextY)
                                nextY = other.position.y;
                        }
                    }

                    // Check +Z ray
                    if (other.position.z >= zTop) {
                        if (cx >= other.position.x && cx <= other.position.x + other.size.x &&
                                cy >= other.position.y && cy <= other.position.y + other.size.y) {
                            if (other.position.z < nextZ)
                                nextZ = other.position.z;
                        }
                    }
                }

                float emptySpaceX = nextX - xRight;
                float emptySpaceY = nextY - yTop;
                float emptySpaceZ = nextZ - zTop;

                float totalEmptySpace = emptySpaceX + emptySpaceY + emptySpaceZ;

                if (totalEmptySpace > maxEmptySpace) {
                    maxEmptySpace = totalEmptySpace;
                    targetBox = box;
                }
            }
        }

        // Target Box found, now swap it randomly
        if (targetBox != null && mutatedOrder.size() > 1) {
            int targetIndex = -1;
            for (int i = 0; i < originalBoxes.size(); i++) {
                if (originalBoxes.get(i).id == targetBox.id) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1)
                targetIndex = targetBox.id; // Fallback

            int orderIndex1 = mutatedOrder.indexOf(targetIndex);

            if (orderIndex1 != -1) {
                int orderIndex2 = random.nextInt(mutatedOrder.size());
                while (orderIndex1 == orderIndex2) {
                    orderIndex2 = random.nextInt(mutatedOrder.size());
                }
                Integer temp = mutatedOrder.get(orderIndex1);
                mutatedOrder.set(orderIndex1, mutatedOrder.get(orderIndex2));
                mutatedOrder.set(orderIndex2, temp);
            }
        }

        return mutatedOrder;
    }
}
