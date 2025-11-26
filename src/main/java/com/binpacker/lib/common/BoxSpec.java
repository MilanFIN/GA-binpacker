package com.binpacker.lib.common;

public class BoxSpec {
	public int id = 0;
	public Point3f position;
	public Point3f size;

	public BoxSpec(int id, Point3f position, Point3f size) {
		this.id = id;
		this.position = position;
		this.size = size;
	}

	public BoxSpec(Point3f position, Point3f size) {
		this.position = position;
		this.size = size;
	}

	@Override
	public String toString() {
		return String.format("BoxSpec(pos=%s, size=%s)", position, size);
	}

	public double getVolume() {
		return size.x * size.y * size.z;
	}
}
