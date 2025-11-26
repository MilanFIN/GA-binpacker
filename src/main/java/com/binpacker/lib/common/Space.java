package com.binpacker.lib.common;

public class Space {
	public float x, y, z, w, h, d;

	public Space(float x, float y, float z, float w, float h, float d) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		this.h = h;
		this.d = d;
	}

	// 2D constructor for backward compatibility (z=0, d=0? or maybe
	// d=infinity/large?)
	// For 2D packing, we usually ignore Z. Let's set z=0 and d=0.
	public Space(float x, float y, float w, float h) {
		this(x, y, 0, w, h, 0);
	}

	@Override
	public String toString() {
		return String.format("Space(x=%.2f, y=%.2f, z=%.2f, w=%.2f, h=%.2f, d=%.2f)", x, y, z, w, h, d);
	}
}
