package com.binpacker.lib.optimizer.mutators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.optimizer.Solution;

public class InsertMutation {

    public static List<Integer> modify(Random random, Solution currentSequence, Solution second, Bin bin, List<Box> originalBoxes) {
        List<Integer> mutatedOrder = new ArrayList<>(currentSequence.order);
        
        if (mutatedOrder.size() > 1) {
            int removeIndex = random.nextInt(mutatedOrder.size());
            Integer temp = mutatedOrder.remove(removeIndex);
            
            int insertIndex = random.nextInt(mutatedOrder.size() + 1); // +1 because we can insert at the very end
            mutatedOrder.add(insertIndex, temp);
        }
        
        return mutatedOrder;
    }
}
