package com.binpacker.lib.solver;

import java.util.List;

import com.binpacker.lib.common.Box;

public class MOAB implements Solver {

	@Override
	public List<List<Box>> solve(List<Box> boxes, Box bin) {

		// TODO: make this one, that checks all orientations
		// chooses the one, where the farthest corner is closest to the start of the bin

		// Then we create 3 maximally large slices from the original free space
		// (firstfit doesn't do this in front and on top of the box)
		// after the placement the existing spaces are checked for collision with the
		// newly placed box. Any collisions lead to further splits

		// occasionally we might need to prune spaces to remove ones
		// that are completely covered by other spaces

		// Space unconstrainedFront = new Space(space.x, space.y, space.z + box.size.z,
		// space.w, space.h, space.d - box.size.z);
		// if (unconstrainedFront.w > 0 && unconstrainedFront.h > 0 &&
		// unconstrainedFront.d > 0) {
		// boolean collides = false;
		// for (Box existingBox : bin.boxes) {
		// if (existingBox.collidesWith(placedBox)) {
		// collides = true;
		// break;
		// }
		// }
		// if (!collides) {
		// bin.freeSpaces.add(unconstrainedFront);
		// } else {
		// Space front = new Space(space.x, space.y, space.z + box.size.z,
		// box.size.x, box.size.y, space.d - box.size.z);
		// if (front.w > 0 && front.h > 0 && front.d > 0)
		// bin.freeSpaces.add(front);
		// }
		// }

		throw new UnsupportedOperationException("Unimplemented method 'solve'");
	}

}
