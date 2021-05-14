package com.github.ruediste.gerberLib.linAlg;

public class CoordinateVector {

	public static final CoordinateVector ZERO = of(0, 0);
	public final double x;
	public final double y;

	public static CoordinateVector of(double x, double y) {
		return new CoordinateVector(x, y);
	}

	public static CoordinateVector ofAngular(double r, double angle) {
		return of(r * Math.cos(angle * Math.PI / 180), r * (Math.sin(angle * Math.PI / 180)));
	}

	public CoordinateVector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double length() {
		return Math.sqrt(length2());
	}

	public double length2() {
		return x * x + y * y;
	}

	public CoordinateVector normalize() {
		return scale(1 / length());
	}

	public CoordinateVector scale(double factor) {
		return new CoordinateVector(x * factor, y * factor);
	}

	/**
	 * Return the vector rotated 90 degrees counter clockwise
	 */
	public CoordinateVector normal() {
		return new CoordinateVector(-y, x);
	}

	public CoordinateVector minus(CoordinateVector other) {
		return of(x - other.x, y - other.y);
	}

	public CoordinateVector negate() {
		return scale(-1);
	}

	public CoordinateVector plus(CoordinateVector other) {
		return of(x + other.x, y + other.y);
	}

	/**
	 * Angle in degrees between the vectors as measured in a counterclockwise
	 * direction from this vector to other
	 */
	public double angleTo(CoordinateVector other) {
		double result = 180 / Math.PI * Math.atan2(x * other.y - y * other.x, x * other.x + y * other.y);
		if (result < 0)
			result += 360;
		return result;
	}

	/**
	 * Angle in degrees between (1,0) and this vector measured in a counterclockwise
	 * direction
	 */
	public double angle() {
		double result = 180 / Math.PI * Math.atan2(y, x);
		if (result < 0)
			result += 360;
		return result;
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

	public CoordinateVector minus(double dx, double dy) {
		return minus(CoordinateVector.of(dx, dy));
	}

	public CoordinateVector minusX(double dx) {
		return minus(dx, 0);
	}

	public CoordinateVector minusY(double dy) {
		return minus(0, dy);
	}

	public CoordinateVector plus(double dx, double dy) {
		return plus(CoordinateVector.of(dx, dy));
	}

	public CoordinateVector plusX(double dx) {
		return plus(dx, 0);
	}

	public CoordinateVector plusY(double dy) {
		return plus(0, dy);
	}

	public double dotProduct(CoordinateVector other) {
		return x * other.x + y * other.y;
	}

	/**
	 * Rotate vector by angle counter clockwise in degrees
	 */
	public CoordinateVector rotate(double angle) {
		double a = angle / 180 * Math.PI;
		return of(Math.cos(a) * x - Math.sin(a) * y, Math.sin(a) * x + Math.cos(a) * y);
	}

}
