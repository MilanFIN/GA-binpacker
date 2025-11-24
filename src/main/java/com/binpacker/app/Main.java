package com.binpacker.app;

import com.binpacker.lib.BoxSpec;
import com.binpacker.lib.Point3f;
import com.binpacker.lib.FirstFit3D;
import com.binpacker.lib.Solver;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
	public static void main(String[] args) {
		List<BoxSpec> boxes = new ArrayList<>();
		// Create some sample boxes
		boxes = generateRandomBoxes(100);

		System.out.println("Starting solver...");
		Solver solver = new FirstFit3D();
		// Define a bin size, e.g., 5x5x5
		BoxSpec bin = new BoxSpec(new Point3f(0, 0, 0), new Point3f(30, 30, 30));
		List<List<BoxSpec>> bins = solver.solve(boxes, bin);

		System.out.println("Solver finished. Result:");
		for (int i = 0; i < bins.size(); i++) {
			System.out.println("Bin " + i + ":");
			for (BoxSpec box : bins.get(i)) {
				System.out.println("  " + box);
			}
		}
	}

	private static List<BoxSpec> generateRandomBoxes(int count) {
		List<BoxSpec> boxes = new ArrayList<>();
		Random random = new Random();
		for (int i = 0; i < count; i++) {
			float width = random.nextInt(10) + 1; // Random size between 1 and 10
			float height = random.nextInt(10) + 1; // Random size between 1 and 10
			float depth = random.nextInt(10) + 1; // Random size between 1 and 10
			// Position is set to (0,0,0) as the solver will determine placement
			boxes.add(new BoxSpec(new Point3f(0, 0, 0), new Point3f(width, height, depth)));
		}
		return boxes;
	}

}
