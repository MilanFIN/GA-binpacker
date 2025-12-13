package com.binpacker.lib.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.solver.SolverInterface;

public abstract class Optimizer {

	private SolverInterface solver;
	protected List<Box> boxes;
	private Bin bin;

	protected List<List<Integer>> boxOrders; // Population
	private Random random = new Random();
	protected int populationSize;
	private int eliteCount;
	protected boolean growingBin;
	protected String growAxis;

	protected boolean threaded;

	protected abstract List<Integer> crossOver(List<Integer> parent1, List<Integer> parent2);

	protected abstract List<Integer> mutate(List<Integer> order);

	public abstract double rate(List<List<Box>> solution, Bin bin);

	// ---- Initialize ----
	public void initialize(SolverInterface solver, List<Box> boxes, Bin bin, boolean growingBin, String growAxis,
			int populationSize,
			int eliteCount, boolean threaded) {
		this.solver = solver;
		this.boxes = boxes;
		this.bin = bin;
		this.growingBin = growingBin;
		this.growAxis = growAxis;
		this.populationSize = populationSize;
		this.eliteCount = eliteCount;
		this.threaded = threaded;

		generateInitialPopulation();
	}

	public void generateInitialPopulation() {
		boxOrders = new ArrayList<>();

		List<Integer> base = new ArrayList<>();
		for (int i = 0; i < boxes.size(); i++)
			base.add(i);

		for (int i = 0; i < populationSize; i++) {
			List<Integer> order = new ArrayList<>(base);
			Collections.shuffle(order, random);
			boxOrders.add(order);
		}
	}

	// ---- Main GA Logic ----
	public List<List<Box>> executeNextGeneration() {

		List<Solution> scored = new ArrayList<>();

		int numThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		List<Future<Solution>> futures = new ArrayList<>();

		if (this.threaded) {
			for (List<Integer> order : boxOrders) {
				futures.add(executor.submit(() -> {
					List<Box> orderedBoxes = applyOrder(order);
					List<List<Box>> solved = solver.solve(orderedBoxes);
					double score = rate(solved, this.bin);
					return new Solution(order, score, solved);
				}));
			}

			for (Future<Solution> future : futures) {
				try {
					scored.add(future.get());
				} catch (InterruptedException | ExecutionException e) {
					Thread.currentThread().interrupt(); // Restore interrupt status
					// Handle or log the exception, e.g., System.err.println("Error processing task:
					// " + e.getMessage());
				}
			}

			executor.shutdown();
			try {
				if (!executor.awaitTermination(3, TimeUnit.MINUTES)) { // Wait for tasks to complete
					// Optionally, force shutdown if tasks don't complete in time
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				executor.shutdownNow(); // Cancel currently executing tasks
			}
		} else {
			for (List<Integer> order : boxOrders) {
				List<Box> orderedBoxes = applyOrder(order);
				List<List<Box>> solved = solver.solve(orderedBoxes);
				double score = rate(solved, this.bin);
				scored.add(new Solution(order, score, solved));
			}
		}

		// Sort best to worst, order is reverse when packing to a single bin
		// (lower height is better)
		if (!growingBin) {
			scored.sort(Comparator.comparingDouble(s -> -s.score));
		} else {
			scored.sort(Comparator.comparingDouble(s -> s.score));
		}

		// Best solution of this generation → returned
		List<List<Box>> bestSolution = scored.get(0).solved;

		// ---------------------------------------------------------
		// Build next generation
		// ---------------------------------------------------------
		List<List<Integer>> nextGen = new ArrayList<>();

		// 1. Keep the elite (top 20%)
		for (int i = 0; i < eliteCount; i++) {
			nextGen.add(new ArrayList<>(scored.get(i).order));
		}

		// 2. Fill remaining 80% with crossover or mutation
		while (nextGen.size() < populationSize) {

			if (random.nextBoolean()) {
				// crossover
				List<Integer> p1 = scored.get(random.nextInt(eliteCount)).order;
				List<Integer> p2 = scored.get(random.nextInt(eliteCount)).order;
				nextGen.add(crossOver(p1, p2));
			} else {
				// mutation
				List<Integer> p = scored.get(random.nextInt(eliteCount)).order;
				nextGen.add(mutate(p));
			}
		}

		// Replace population and increment generation counter
		this.boxOrders = nextGen;

		return bestSolution;
	}

	public void release() {
		this.solver.release();
	}

	// --- Helper: apply an index order to the box list ---
	private List<Box> applyOrder(List<Integer> order) {
		List<Box> result = new ArrayList<>();
		for (Integer idx : order)
			result.add(boxes.get(idx));
		return result;
	}

}
