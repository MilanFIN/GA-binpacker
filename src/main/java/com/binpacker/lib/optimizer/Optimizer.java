package com.binpacker.lib.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.optimizer.mutators.BinPreservationCrossover;
import com.binpacker.lib.optimizer.mutators.CrossOver;
import com.binpacker.lib.optimizer.mutators.InsertMutation;
import com.binpacker.lib.optimizer.mutators.Modifier;
import com.binpacker.lib.optimizer.mutators.ScrambleMutation;
import com.binpacker.lib.optimizer.mutators.SpaceMutation;
import com.binpacker.lib.optimizer.mutators.SwapMutation;

public abstract class Optimizer<S> {

	protected S solverSource;
	protected List<Box> boxes;
	protected Bin bin;

	protected List<List<Integer>> boxOrders; // Population
	protected Random random = new Random();
	protected int populationSize;
	private int eliteCount;
	protected boolean growingBin;
	protected String growAxis;
	protected List<Integer> rotationAxes;

	protected int threads; // 0 for max

	protected List<Modifier> modifiers = new ArrayList<>();

	protected abstract List<Solution> evaluatePopulation(List<List<Integer>> population);

	protected abstract List<List<Box>> finalizeBestSolution(Solution bestSolution);

	public abstract double rate(List<List<Box>> solution, Bin bin);

	// ---- Initialize ----
	public void initialize(S solverSource, List<Box> boxes, Bin bin, boolean growingBin,
			String growAxis, List<Integer> rotationAxes,
			int populationSize,
			int eliteCount, int threads) {
		this.solverSource = solverSource;
		this.boxes = boxes;
		this.bin = bin;
		this.growingBin = growingBin;
		this.growAxis = growAxis;
		this.rotationAxes = rotationAxes;
		this.populationSize = populationSize;
		this.eliteCount = eliteCount;
		this.threads = threads;

		if (this.modifiers.isEmpty()) {
			this.modifiers.add(CrossOver::modify);
			this.modifiers.add(SwapMutation::modify);
			this.modifiers.add(SpaceMutation::modify);
			this.modifiers.add(InsertMutation::modify);
			this.modifiers.add(BinPreservationCrossover::modify);
			this.modifiers.add(ScrambleMutation::modify);
		}

		generateInitialPopulation();
	}

	public void generateInitialPopulation() {
		boxOrders = new ArrayList<>();

		List<Integer> base = new ArrayList<>();
		for (int i = 0; i < boxes.size(); i++)
			base.add(i);

		// // First order: growing by volume
		List<Integer> growingOrder = new ArrayList<>(base);
		Collections.sort(growingOrder,
				(i1, i2) -> Double.compare(boxes.get(i1).getVolume(),
						boxes.get(i2).getVolume()));
		boxOrders.add(growingOrder);

		// Second order: shrinking by volume
		List<Integer> shrinkingOrder = new ArrayList<>(base);
		Collections.sort(shrinkingOrder,
				(i1, i2) -> Double.compare(boxes.get(i2).getVolume(), boxes.get(i1).getVolume()));
		boxOrders.add(shrinkingOrder);

		// third order: shrinking by longest side
		List<Integer> shrinkingLongestOrder = new ArrayList<>(base);
		Collections.sort(shrinkingLongestOrder,
				(i1, i2) -> Double.compare(boxes.get(i2).getLongestSide(), boxes.get(i1).getLongestSide()));
		boxOrders.add(shrinkingLongestOrder);

		// Remaining orders: random
		for (int i = 2; i < populationSize; i++) {
			List<Integer> order = new ArrayList<>(base);
			Collections.shuffle(order, random);
			boxOrders.add(order);
		}

		this.populationSize = boxOrders.size();
	}

	// ---- Main GA Logic ----
	public List<List<Box>> executeNextGeneration() {

		// 1. Evaluate current population
		List<Solution> scored = evaluatePopulation(boxOrders);

		// 2. Sort best to worst
		if (!growingBin) {
			scored.sort(Comparator.comparingDouble(s -> -s.score));
		} else {
			scored.sort(Comparator.comparingDouble(s -> s.score));
		}

		// 3. Get best solution of this generation
		Solution bestOfGen = scored.get(0);
		List<List<Box>> bestSolutionPack = finalizeBestSolution(bestOfGen);

		// ---------------------------------------------------------
		// Build next generation
		// ---------------------------------------------------------
		List<List<Integer>> nextGen = new ArrayList<>();

		// Keep elite
		for (int i = 0; i < eliteCount && i < scored.size(); i++) {
			nextGen.add(new ArrayList<>(scored.get(i).order));
		}

		// Fill remaining
		while (nextGen.size() < populationSize) {
			Modifier modifier = modifiers.get(random.nextInt(modifiers.size()));

			// We must breed from the ELITE solutions to improve score, not the worst ones!
			int maxElite = Math.max(1, Math.min(eliteCount, scored.size()));
			Solution currentSequence = scored.get(random.nextInt(maxElite));
			Solution secondSequence = scored.get(random.nextInt(maxElite));

			nextGen.add(modifier.modify(random, currentSequence, secondSequence, this.bin, this.boxes));
		}

		// Replace population
		this.boxOrders = nextGen;

		return bestSolutionPack;
	}

	public void release() {
		// Default no-op
	}

	// --- Helper: apply an index order to the box list ---
	protected List<Box> applyOrder(List<Integer> order) {
		List<Box> result = new ArrayList<>();
		for (Integer idx : order)
			result.add(boxes.get(idx));
		return result;
	}

}
