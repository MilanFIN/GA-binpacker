package com.binpacker.lib.optimizer.mutators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SwapMutation {

    public static List<Integer> modify(Random random, List<Integer> currentSequence, List<Integer> second) {
        List<Integer> mutatedOrder = new ArrayList<>(currentSequence);
        int index1 = random.nextInt(mutatedOrder.size());
        int index2 = random.nextInt(mutatedOrder.size());
        while (index1 == index2) {
            index2 = random.nextInt(mutatedOrder.size());
        }
        Collections.swap(mutatedOrder, index1, index2);
        return mutatedOrder;
    }
}
