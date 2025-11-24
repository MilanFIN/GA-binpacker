package com.binpacker.lib;

public class Point3f {
	public float x;
	public float y;
	public float z;

	public Point3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public String toString() {
		return String.format("Point3f(x=%.2f, y=%.2f, z=%.2f)", x, y, z);
	}
}
