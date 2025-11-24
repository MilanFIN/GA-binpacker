package com.binpacker.lib;

import java.util.ArrayList;
import java.util.List;

public class FirstFit2D implements Solver {

	private static class BinContext {
		List<BoxSpec> boxes = new ArrayList<>();
		List<Space> freeSpaces = new ArrayList<>();

		BinContext(BoxSpec binTemplate) {
			// Initial free space is the whole bin (Z is ignored/0)
			freeSpaces.add(new Space(0, 0, binTemplate.size.x, binTemplate.size.y));
		}
	}

	@Override
	public List<List<BoxSpec>> solve(List<BoxSpec> boxes, BoxSpec binTemplate) {
		List<BinContext> activeBins = new ArrayList<>();
		List<List<BoxSpec>> result = new ArrayList<>();

		// Start with one bin
		activeBins.add(new BinContext(binTemplate));

		for (BoxSpec box : boxes) {
			boolean placed = false;

			// Try to fit in any existing bin
			for (BinContext bin : activeBins) {
				for (int i = 0; i < bin.freeSpaces.size(); i++) {
					Space space = bin.freeSpaces.get(i);
					if (canFit(box, space)) {
						placeBox(box, bin, i);
						placed = true;
						break;
					}
				}
				if (placed)
					break;
			}

			if (!placed) {
				// Create new bin
				BinContext newBin = new BinContext(binTemplate);
				activeBins.add(newBin);
				// Try to place in the new bin
				if (canFit(box, newBin.freeSpaces.get(0))) {
					placeBox(box, newBin, 0);
				} else {
					System.err.println("Box too big for bin: " + box);
				}
			}
		}

		// Collect results
		for (BinContext bin : activeBins) {
			result.add(bin.boxes);
		}

		return result;
	}

	private boolean canFit(BoxSpec box, Space space) {
		return box.size.x <= space.w && box.size.y <= space.h;
	}

	private void placeBox(BoxSpec box, BinContext bin, int spaceIndex) {
		Space space = bin.freeSpaces.get(spaceIndex);

		// Create placed box
		BoxSpec placedBox = new BoxSpec(
				new Point3f(space.x, space.y, 0), // Z is 0 for 2D
				new Point3f(box.size.x, box.size.y, box.size.z));
		bin.boxes.add(placedBox);

		// Remove the used space
		bin.freeSpaces.remove(spaceIndex);

		// Split the remaining space
		Space top = new Space(space.x, space.y + box.size.y, space.w, space.h - box.size.y);
		Space rightSide = new Space(space.x + box.size.x, space.y, space.w - box.size.x, box.size.y);

		if (top.w > 0 && top.h > 0)
			bin.freeSpaces.add(top);
		if (rightSide.w > 0 && rightSide.h > 0)
			bin.freeSpaces.add(rightSide);
	}
}
