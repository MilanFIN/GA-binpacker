package com.binpacker.lib.optimizer.mutators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.optimizer.Solution;

public class ScrambleMutation {

    public static List<Integer> modify(Random random, Solution currentSequence, Solution second, Bin bin, List<Box> originalBoxes) {
        List<Integer> mutatedOrder = new ArrayList<>(currentSequence.order);
        
        int size = mutatedOrder.size();
        if (size > 1) {
            int start = random.nextInt(size);
            int end = random.nextInt(size);
            
            if (start > end) {
                int temp = start;
                start = end;
                end = temp;
            }
            
            // Sublist is a view, shuffling it shuffles the backing list directly!
            List<Integer> sublist = mutatedOrder.subList(start, end + 1);
            Collections.shuffle(sublist, random);
        }
        
        return mutatedOrder;
    }
}
