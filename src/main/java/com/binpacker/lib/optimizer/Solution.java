package com.binpacker.lib.optimizer;

import java.util.List;

import com.binpacker.lib.common.Box;

public class Solution {
	public final List<Integer> order;
	public final double score;
	public final List<List<Box>> solved;

	public Solution(List<Integer> order, double score, List<List<Box>> solved) {
		this.order = order;
		this.score = score;
		this.solved = solved;
	}
}