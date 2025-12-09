package com.binpacker.lib.solver.common;

import com.binpacker.lib.common.Bin;

public class SolverProperties {
	public Bin bin;
	public boolean growingBin;
	public String growAxis;

	public SolverProperties(Bin bin, boolean growingBin, String growAxis) {
		this.bin = bin;
		this.growingBin = growingBin;
		this.growAxis = growAxis;
	}

}
