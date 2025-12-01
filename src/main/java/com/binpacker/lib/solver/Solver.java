package com.binpacker.lib.solver;

import java.util.List;

import com.binpacker.lib.common.Box;

public interface Solver {
	List<List<Box>> solve(List<Box> boxes, Box bin, boolean growingBin, String growAxis);
}
