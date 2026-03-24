package com.binpacker.lib.optimizer.mutators;

import java.util.List;
import java.util.Random;

@FunctionalInterface
public interface Modifier {
    List<Integer> modify(Random random, List<Integer> current, List<Integer> second);
}
