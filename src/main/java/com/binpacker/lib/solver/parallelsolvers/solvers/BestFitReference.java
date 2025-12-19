package com.binpacker.lib.solver.parallelsolvers.solvers;

import java.util.ArrayList;
import java.util.List;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.common.Point3f;
import com.binpacker.lib.common.Space;

/**
 * Best-fit reference solver for reconstructing packing solutions.
 * Selects the space with minimum waste (tightest fit) for each box.
 */
public class BestFitReference implements ReferenceSolver {

	@Override
	public List<Bin> solve(List<Box> boxes, List<Integer> order, Bin binTemplate) {
		List<Bin> activeBins = new ArrayList<>();

		// Initialize first bin
		activeBins.add(new Bin(0, binTemplate.w, binTemplate.h, binTemplate.d));

		// Iterate through boxes in the given order
		for (int boxIndex : order) {
			Box originalBox = boxes.get(boxIndex);
			// Create a copy of the box to store placement
			Box box = new Box(originalBox.id, new Point3f(0, 0, 0),
					new Point3f(originalBox.size.x, originalBox.size.y, originalBox.size.z));

			boolean placed = false;

			// Best-fit: find the smallest fitting space across all bins
			int bestBin = -1;
			int bestSpace = -1;
			double bestWaste = Double.POSITIVE_INFINITY;

			// Try to fit in existing bins
			for (int b = 0; b < activeBins.size(); b++) {
				Bin bin = activeBins.get(b);
				List<Space> spaces = bin.freeSpaces;

				// Iterate spaces in bin
				for (int s = 0; s < spaces.size(); s++) {
					Space sp = spaces.get(s);

					if (box.size.x <= sp.w && box.size.y <= sp.h && box.size.z <= sp.d) {
						// Calculate waste (space volume - box volume)
						double spaceVolume = sp.w * sp.h * sp.d;
						double boxVolume = box.size.x * box.size.y * box.size.z;
						double waste = spaceVolume - boxVolume;

						// Update best fit if this space has less waste
						if (waste < bestWaste) {
							bestWaste = waste;
							bestBin = b;
							bestSpace = s;
						}
					}
				}
			}

			// Place box in best space if found
			if (bestBin >= 0) {
				placed = true;
				Bin bin = activeBins.get(bestBin);
				List<Space> spaces = bin.freeSpaces;
				Space sp = spaces.get(bestSpace);

				// Set position
				box.position.x = sp.x;
				box.position.y = sp.y;
				box.position.z = sp.z;
				bin.boxes.add(box);

				// Remove used space (swap with last for efficiency)
				int lastIdx = spaces.size() - 1;
				if (bestSpace != lastIdx) {
					spaces.set(bestSpace, spaces.get(lastIdx));
				}
				spaces.remove(lastIdx);

				// Create new spaces (Guillotine Split)
				// Right
				if (sp.w - box.size.x > 0) {
					spaces.add(new Space(
							sp.x + box.size.x, sp.y, sp.z,
							sp.w - box.size.x, sp.h, sp.d));
				}
				// Top
				if (sp.h - box.size.y > 0) {
					spaces.add(new Space(
							sp.x, sp.y + box.size.y, sp.z,
							box.size.x, sp.h - box.size.y, sp.d));
				}
				// Front
				if (sp.d - box.size.z > 0) {
					spaces.add(new Space(
							sp.x, sp.y, sp.z + box.size.z,
							box.size.x, box.size.y, sp.d - box.size.z));
				}
			}

			// If not placed, create new bin
			if (!placed) {
				Bin newBin = new Bin(activeBins.size(), binTemplate.w, binTemplate.h, binTemplate.d);
				activeBins.add(newBin);

				Space sp = newBin.freeSpaces.get(0); // The single initial space

				if (box.size.x <= sp.w && box.size.y <= sp.h && box.size.z <= sp.d) {
					box.position.x = sp.x;
					box.position.y = sp.y;
					box.position.z = sp.z;
					newBin.boxes.add(box);

					newBin.freeSpaces.remove(0);

					// Right
					if (sp.w - box.size.x > 0) {
						newBin.freeSpaces.add(new Space(
								sp.x + box.size.x, sp.y, sp.z,
								sp.w - box.size.x, sp.h, sp.d));
					}
					// Top
					if (sp.h - box.size.y > 0) {
						newBin.freeSpaces.add(new Space(
								sp.x, sp.y + box.size.y, sp.z,
								box.size.x, sp.h - box.size.y, sp.d));
					}
					// Front
					if (sp.d - box.size.z > 0) {
						newBin.freeSpaces.add(new Space(
								sp.x, sp.y, sp.z + box.size.z,
								box.size.x, box.size.y, sp.d - box.size.z));
					}
				} else {
					// Should not happen if box fits in bin template
					System.err.println("Box " + box.id + " too large for bin template!");
				}
			}
		}

		return activeBins;
	}

}
