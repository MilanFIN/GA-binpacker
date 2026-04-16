package com.binpacker.lib.solver.parallelsolvers;

import java.util.ArrayList;
import java.util.List;

import com.binpacker.lib.common.Bin;
import com.binpacker.lib.common.Box;
import com.binpacker.lib.common.Point3f;
import com.binpacker.lib.common.Space;

/**
 * First-fit EMS reference solver for reconstructing packing solutions.
 * Directly translates the logic from firstfit_ems.cl to Java.
 */
public class FirstFitEMSReference implements ReferenceSolver {

	@Override
	public List<Bin> solve(List<Box> boxes, List<Integer> order,
			com.binpacker.lib.solver.common.SolverProperties properties) {
		List<Bin> activeBins = new ArrayList<>();
		Bin binTemplate = properties.bin;
		List<Integer> allowedRotations = properties.rotationAxes;

		// Initialize first bin
		activeBins.add(new Bin(0, binTemplate.w, binTemplate.h, binTemplate.d, binTemplate.maxWeight));

		// Iterate through boxes in the given order
		for (int boxIndex : order) {
			Box originalBox = boxes.get(boxIndex);

			// Create a local working copy of the box
			Box box = new Box(originalBox.id, new Point3f(0, 0, 0),
					new Point3f(originalBox.size.x, originalBox.size.y, originalBox.size.z), originalBox.weight);

			boolean placed = false;

			// First-fit parameters
			int firstBinIndex = -1;
			int firstSpaceIndex = -1;
			int firstOrientation = -1;

			// Define valid orientations
			// {w, h, d}
			float[][] orientations = {
					{ box.size.x, box.size.y, box.size.z }, // 0: original
					{ box.size.x, box.size.z, box.size.y }, // 1: rotate around x
					{ box.size.y, box.size.x, box.size.z }, // 2: rotate around z
					{ box.size.z, box.size.y, box.size.x } // 3: diagonal 2
			};

			// 1. Find First Fit
			for (int b = 0; b < activeBins.size(); b++) {
				Bin bin = activeBins.get(b);

				// Skip bin if weight limit would be exceeded
				if (bin.maxWeight > 0 && bin.weight + box.weight > bin.maxWeight) {
					continue;
				}

				List<Space> spaces = bin.freeSpaces;

				for (int s = 0; s < spaces.size(); s++) {
					Space sp = spaces.get(s);

					// Try all orientations
					for (int o = 0; o < 4; o++) {
						// Filter rotations
						if (o == 1 && (allowedRotations == null || !allowedRotations.contains(0)))
							continue;
						if (o == 2 && (allowedRotations == null || !allowedRotations.contains(1)))
							continue;
						if (o == 3 && (allowedRotations == null || !allowedRotations.contains(2)))
							continue;

						float w = orientations[o][0];
						float h = orientations[o][1];
						float d = orientations[o][2];

						if (w <= sp.w && h <= sp.h && d <= sp.d) {
							firstBinIndex = b;
							firstSpaceIndex = s;
							firstOrientation = o;
							break; // Break orientation loop
						}
					}
					if (firstBinIndex != -1) {
						break; // Break space loop
					}
				}

				if (firstBinIndex != -1) {
					break; // Break bin loop
				}
			}

			// 2. Place Box
			if (firstBinIndex >= 0) {
				placed = true;
				Bin bin = activeBins.get(firstBinIndex);
				List<Space> spaces = bin.freeSpaces;
				Space sp = spaces.get(firstSpaceIndex); // The space we are placing into (copy reference)

				// Get dimensions
				float boxW = orientations[firstOrientation][0];
				float boxH = orientations[firstOrientation][1];
				float boxD = orientations[firstOrientation][2];

				float boxX = sp.x;
				float boxY = sp.y;
				float boxZ = sp.z;

				// Update box props and add to bin
				box.size.x = boxW;
				box.size.y = boxH;
				box.size.z = boxD;
				box.position.x = boxX;
				box.position.y = boxY;
				box.position.z = boxZ;
				bin.boxes.add(box);
				bin.weight += box.weight;

				// Remove the used space
				int lastIdx = spaces.size() - 1;
				if (firstSpaceIndex != lastIdx) {
					spaces.set(firstSpaceIndex, spaces.get(lastIdx));
				}
				spaces.remove(lastIdx);

				// A. Add splits from the placed space (BSP-style)
				// Right
				if (sp.w - boxW > 0.0f) {
					spaces.add(new Space(sp.x + boxW, sp.y, sp.z, sp.w - boxW, sp.h, sp.d));
				}
				// Top
				if (sp.h - boxH > 0.0f) {
					spaces.add(new Space(sp.x, sp.y + boxH, sp.z, sp.w, sp.h - boxH, sp.d));
				}
				// Front
				if (sp.d - boxD > 0.0f) {
					spaces.add(new Space(sp.x, sp.y, sp.z + boxD, sp.w, sp.h, sp.d - boxD));
				}

				// B. Prune intersecting spaces (EMS)
				// Loop backwards
				for (int k = spaces.size() - 1; k >= 0; k--) {
					Space other = spaces.get(k);

					if (checkCollision(boxX, boxY, boxZ, boxW, boxH, boxD, other)) {
						// Remove other space
						int lastK = spaces.size() - 1;
						if (k != lastK) {
							spaces.set(k, spaces.get(lastK));
						}
						spaces.remove(lastK);

						// Split other space into up to 6 new spaces
						// 1. Right
						if (boxX + boxW < other.x + other.w) {
							spaces.add(new Space(
									boxX + boxW, other.y, other.z,
									(other.x + other.w) - (boxX + boxW), other.h, other.d));
						}
						// 2. Left
						if (boxX > other.x) {
							spaces.add(new Space(
									other.x, other.y, other.z,
									boxX - other.x, other.h, other.d));
						}
						// 3. Top
						if (boxY + boxH < other.y + other.h) {
							spaces.add(new Space(
									other.x, boxY + boxH, other.z,
									other.w, (other.y + other.h) - (boxY + boxH), other.d));
						}
						// 4. Bottom
						if (boxY > other.y) {
							spaces.add(new Space(
									other.x, other.y, other.z,
									other.w, boxY - other.y, other.d));
						}
						// 5. Front
						if (boxZ + boxD < other.z + other.d) {
							spaces.add(new Space(
									other.x, other.y, boxZ + boxD,
									other.w, other.h, (other.z + other.d) - (boxZ + boxD)));
						}
						// 6. Back
						if (boxZ > other.z) {
							spaces.add(new Space(
									other.x, other.y, other.z,
									other.w, other.h, boxZ - other.z));
						}
					}
				}

				// C. Prune Contained Spaces
				for (int i = spaces.size() - 1; i >= 0; i--) {
					if (i >= spaces.size())
						continue;

					Space s1 = spaces.get(i);
					if (s1.w <= 0.0f || s1.h <= 0.0f || s1.d <= 0.0f) {
						int lastI = spaces.size() - 1;
						if (i != lastI)
							spaces.set(i, spaces.get(lastI));
						spaces.remove(lastI);
						continue;
					}

					boolean contained = false;
					for (int j = 0; j < spaces.size(); j++) {
						if (i == j)
							continue;
						if (isContained(s1, spaces.get(j))) {
							contained = true;
							break;
						}
					}

					if (contained) {
						int lastI = spaces.size() - 1;
						if (i != lastI)
							spaces.set(i, spaces.get(lastI));
						spaces.remove(lastI);
					}
				}

			}

			// 3. New Bin
			if (!placed) {
				// Determine orientation for new bin (first that fits)
				int newBinOrientation = -1; // default
				for (int o = 0; o < 4; o++) {
					// Filter rotations
					if (o == 1 && (allowedRotations == null || !allowedRotations.contains(0)))
						continue;
					if (o == 2 && (allowedRotations == null || !allowedRotations.contains(1)))
						continue;
					if (o == 3 && (allowedRotations == null || !allowedRotations.contains(2)))
						continue;

					float w = orientations[o][0];
					float h = orientations[o][1];
					float d = orientations[o][2];
					if (w <= binTemplate.w && h <= binTemplate.h && d <= binTemplate.d) {
						newBinOrientation = o;
						break;
					}
				}

				if (newBinOrientation != -1) {
					float boxW = orientations[newBinOrientation][0];
					float boxH = orientations[newBinOrientation][1];
					float boxD = orientations[newBinOrientation][2];

					Bin newBin = new Bin(activeBins.size(), binTemplate.w, binTemplate.h, binTemplate.d,
							binTemplate.maxWeight);
					activeBins.add(newBin);
					List<Space> spaces = newBin.freeSpaces;
					spaces.clear();

					// Add box
					box.size.x = boxW;
					box.size.y = boxH;
					box.size.z = boxD;
					box.position.x = 0;
					box.position.y = 0;
					box.position.z = 0;
					newBin.boxes.add(box);
					newBin.weight += box.weight;

					// Initial spaces (EMS style)
					// Right
					if (boxW < binTemplate.w) {
						spaces.add(new Space(boxW, 0.0f, 0.0f, binTemplate.w - boxW, binTemplate.h, binTemplate.d));
					}
					// Top
					if (boxH < binTemplate.h) {
						spaces.add(new Space(0.0f, boxH, 0.0f, binTemplate.w, binTemplate.h - boxH, binTemplate.d));
					}
					// Front
					if (boxD < binTemplate.d) {
						spaces.add(new Space(0.0f, 0.0f, boxD, binTemplate.w, binTemplate.h, binTemplate.d - boxD));
					}
				}
			}
		}

		return activeBins;
	}

	private boolean checkCollision(float bx, float by, float bz, float bw, float bh, float bd, Space s) {
		return (bx < s.x + s.w &&
				bx + bw > s.x &&
				by < s.y + s.h &&
				by + bh > s.y &&
				bz < s.z + s.d &&
				bz + bd > s.z);
	}

	private boolean isContained(Space s1, Space s2) {
		return (s1.x >= s2.x &&
				s1.y >= s2.y &&
				s1.z >= s2.z &&
				s1.x + s1.w <= s2.x + s2.w &&
				s1.y + s1.h <= s2.y + s2.h &&
				s1.z + s1.d <= s2.z + s2.d);
	}
}
