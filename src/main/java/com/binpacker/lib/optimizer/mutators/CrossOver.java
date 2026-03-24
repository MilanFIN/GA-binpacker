package com.binpacker.lib.optimizer.mutators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CrossOver {

    public static List<Integer> modify(Random random, List<Integer> currentSequence, List<Integer> second) {
        List<Integer> parent1 = currentSequence;
        List<Integer> parent2 = second;
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
