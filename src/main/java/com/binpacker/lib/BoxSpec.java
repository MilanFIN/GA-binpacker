package com.binpacker.lib;

public class BoxSpec {
	public Point3f position;
	public Point3f size;

	public BoxSpec(Point3f position, Point3f size) {
		this.position = position;
		this.size = size;
	}

	@Override
	public String toString() {
		return String.format("BoxSpec(pos=%s, size=%s)", position, size);
	}
}
