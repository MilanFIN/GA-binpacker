package com.binpacker.lib.optimizer.mutators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.optimizer.Solution;

public class CrossOver {

    public static List<Integer> modify(Random random, Solution currentSequence, Solution second, Bin bin, List<Box> originalBoxes) {
        List<Integer> parent1 = currentSequence.order;
        List<Integer> parent2 = second.order;
        int size = parent1.size();
        int cut1 = random.nextInt(size);
        int cut2 = random.nextInt(size);

        if (cut1 > cut2) {
            int t = cut1;
            cut1 = cut2;
            cut2 = t;
        }

        List<Integer> child = new ArrayList<>(Collections.nCopies(size, null));

        // 1. Copy the slice from parent2
        for (int i = cut1; i <= cut2; i++) {
            child.set(i, parent2.get(i));
        }

        // 2. Fill remaining positions from parent1 in order
        int fillPos = (cut2 + 1) % size;

        for (int i = 0; i < size; i++) {
            int gene = parent1.get((cut2 + 1 + i) % size);

            if (!child.contains(gene)) {
                child.set(fillPos, gene);
                fillPos = (fillPos + 1) % size;
            }
        }

        return child;
    }
}
