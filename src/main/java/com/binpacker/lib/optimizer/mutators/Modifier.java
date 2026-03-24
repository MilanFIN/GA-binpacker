package com.binpacker.lib.optimizer.mutators;

import java.util.List;
import java.util.Random;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.optimizer.Solution;

@FunctionalInterface
public interface Modifier {
    List<Integer> modify(Random random, Solution current, Solution second, Bin bin, List<Box> originalBoxes);
}
