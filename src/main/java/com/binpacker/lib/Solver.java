package com.binpacker.lib;

import java.util.List;

public interface Solver {
	List<List<BoxSpec>> solve(List<BoxSpec> boxes, BoxSpec bin);
}
