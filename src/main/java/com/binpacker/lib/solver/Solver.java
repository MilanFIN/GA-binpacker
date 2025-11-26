package com.binpacker.lib.solver;

import java.util.List;

import com.binpacker.lib.common.BoxSpec;

public interface Solver {
	List<List<BoxSpec>> solve(List<BoxSpec> boxes, BoxSpec bin);
}
