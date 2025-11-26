package com.binpacker.lib.optimizer;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.binpacker.lib.common.BoxSpec;

public class GAOptimizer extends Optimizer {

	private Random random = new Random();

	@Override
	public double rate(List<List<BoxSpec>> solution, BoxSpec bin) {

		double totalUsedVolume = 0.0;
		int binsToConsider = solution.size() - 1; // Exclude the last bin

		if (binsToConsider <= 0) {
			return 1.0; // No bins to consider or only one bin
		}

		for (int i = 0; i < binsToConsider; i++) {
			List<BoxSpec> currentBinContents = solution.get(i);
			double currentBinUsedVolume = 0.0;
			for (BoxSpec box : currentBinContents) {
				currentBinUsedVolume += box.getVolume();
			}
			totalUsedVolume += currentBinUsedVolume;
		}

		return totalUsedVolume / (binsToConsider * bin.getVolume());

	}

	@Override
	protected List<Integer> crossOver(List<Integer> parent1, List<Integer> parent2) {
		int size = parent1.size();
		Random rand = new Random();

		int cut1 = rand.nextInt(size);
		int cut2 = rand.nextInt(size);

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

	@Override
	protected List<Integer> mutate(List<Integer> order) {

		List<Integer> mutatedOrder = new ArrayList<>(order);
		int index1 = random.nextInt(mutatedOrder.size());
		int index2 = random.nextInt(mutatedOrder.size());
		// Ensure index1 and index2 are different
		while (index1 == index2) {
			index2 = random.nextInt(mutatedOrder.size());
		}
		// Swap elements
		Collections.swap(mutatedOrder, index1, index2);

		return mutatedOrder;

	}
}
