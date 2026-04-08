package com.binpacker.lib.optimizer.mutators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.optimizer.Solution;

public class BinPreservationCrossover {

    public static List<Integer> modify(Random random, Solution currentSequence, Solution second, Bin bin,
            List<Box> originalBoxes) {
        // Find the bin with the highest utilization in parent 1
        double maxUtilization = -1.0;
        List<Box> bestBinBoxes = null;

        if (currentSequence.solved == null) {
            return currentSequence.order;
        }
        for (List<Box> binBoxes : currentSequence.solved) {
            if (binBoxes.isEmpty())
                continue;

            double volume = 0;
            for (Box box : binBoxes) {
                volume += box.getVolume();
            }

            double utilization = volume / bin.getVolume();
            if (utilization > maxUtilization) {
                maxUtilization = utilization;
                bestBinBoxes = binBoxes;
            }
        }

        List<Integer> child = new ArrayList<>();
        Set<Integer> packedIds = new HashSet<>();

        // 1. Pack the best bin from parent 1
        if (bestBinBoxes != null) {
            for (Box box : bestBinBoxes) {
                int targetIndex = -1;
                for (int i = 0; i < originalBoxes.size(); i++) {
                    if (originalBoxes.get(i).id == box.id) {
                        targetIndex = i;
                        break;
                    }
                }
                if (targetIndex == -1)
                    targetIndex = box.id;

                child.add(targetIndex);
                packedIds.add(targetIndex);
            }
        }

        // 2. Fill the rest of the sequence from parent 2
        for (Integer id : second.order) {
            if (!packedIds.contains(id)) {
                child.add(id);
                packedIds.add(id);
            }
        }

        return child;
    }
}
