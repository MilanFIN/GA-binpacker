package com.binpacker.lib.solver;

import java.util.ArrayList;
import java.util.List;

import com.binpacker.lib.common.Box;
import com.binpacker.lib.common.Point3f;
import com.binpacker.lib.common.Space;

public class FirstFit3D implements Solver {

	private static class BinContext {
		List<Box> boxes = new ArrayList<>();
		List<Space> freeSpaces = new ArrayList<>();

		BinContext(Box binTemplate) {
			freeSpaces.add(new Space(0, 0, 0, binTemplate.size.x, binTemplate.size.y, binTemplate.size.z));
		}
	}

	@Override
	public List<List<Box>> solve(List<Box> boxes, Box binTemplate) {
		List<BinContext> activeBins = new ArrayList<>();
		List<List<Box>> result = new ArrayList<>();

		activeBins.add(new BinContext(binTemplate));

		for (Box box : boxes) {
			boolean placed = false;
			for (BinContext bin : activeBins) {
				for (int i = 0; i < bin.freeSpaces.size(); i++) {
					Space space = bin.freeSpaces.get(i);
					Box fittedBox = findFit(box, space);
					if (fittedBox != null) {
						placeBox(fittedBox, bin, i);
						placed = true;
						break;
					}
				}
				if (placed)
					break;
			}

			if (!placed) {
				BinContext newBin = new BinContext(binTemplate);
				activeBins.add(newBin);
				Box fittedBox = findFit(box, newBin.freeSpaces.get(0));
				if (fittedBox != null) {
					placeBox(fittedBox, newBin, 0);
				} else {
					System.err.println("Box too big for bin: " + box);
				}
			}
		}

		for (BinContext bin : activeBins) {
			result.add(bin.boxes);
		}

		return result;
	}

	private Box findFit(Box box, Space space) {
		// Check all 6 orientations (permutations of x, y, z)

		// 1. (x, y, z)
		if (box.size.x <= space.w && box.size.y <= space.h && box.size.z <= space.d) {
			return box;
		}

		// 2. (x, z, y)
		if (box.size.x <= space.w && box.size.z <= space.h && box.size.y <= space.d) {
			return new Box(box.id, box.position, new Point3f(box.size.x, box.size.z, box.size.y));
		}

		// 3. (y, x, z)
		if (box.size.y <= space.w && box.size.x <= space.h && box.size.z <= space.d) {
			return new Box(box.id, box.position, new Point3f(box.size.y, box.size.x, box.size.z));
		}

		// 4. (y, z, x)
		if (box.size.y <= space.w && box.size.z <= space.h && box.size.x <= space.d) {
			return new Box(box.id, box.position, new Point3f(box.size.y, box.size.z, box.size.x));
		}

		// 5. (z, x, y)
		if (box.size.z <= space.w && box.size.x <= space.h && box.size.y <= space.d) {
			return new Box(box.id, box.position, new Point3f(box.size.z, box.size.x, box.size.y));
		}

		// 6. (z, y, x)
		if (box.size.z <= space.w && box.size.y <= space.h && box.size.x <= space.d) {
			return new Box(box.id, box.position, new Point3f(box.size.z, box.size.y, box.size.x));
		}

		return null;
	}

	private void placeBox(Box box, BinContext bin, int spaceIndex) {
		Space space = bin.freeSpaces.get(spaceIndex);

		Box placedBox = new Box(
				box.id,
				new Point3f(space.x, space.y, space.z),
				new Point3f(box.size.x, box.size.y, box.size.z));
		bin.boxes.add(placedBox);

		bin.freeSpaces.remove(spaceIndex);

		Space right = new Space(space.x + box.size.x, space.y, space.z,
				space.w - box.size.x, space.h, space.d);

		Space top = new Space(space.x, space.y + box.size.y, space.z,
				box.size.x, space.h - box.size.y, space.d);

		Space front = new Space(space.x, space.y, space.z + box.size.z,
				box.size.x, box.size.y, space.d - box.size.z);

		if (right.w > 0 && right.h > 0 && right.d > 0)
			bin.freeSpaces.add(right);
		if (top.w > 0 && top.h > 0 && top.d > 0)
			bin.freeSpaces.add(top);
		if (front.w > 0 && front.h > 0 && front.d > 0)
			bin.freeSpaces.add(front);

	}
}
