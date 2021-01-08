package com.github.ruediste.gerberLib.linAlg;

public class CoordinateVector {

	public static final CoordinateVector ZERO = of(CoordinateLength.ZERO, CoordinateLength.ZERO);
	private final CoordinateLengthUnit unit;
	public final CoordinateLength x;
	public final CoordinateLength y;

	public static CoordinateVector of(CoordinateLength x, CoordinateLength y) {
		return new CoordinateVector(x, y);
	}

	public static CoordinateVector of(CoordinateLengthUnit unit, double x, double y) {
		return of(CoordinateLength.of(unit, x), CoordinateLength.of(unit, y));
	}

	public static CoordinateVector of(CoordinateLength r, double angle) {
		return CoordinateVector.of(r.scale(Math.cos(angle * Math.PI / 180)), r.scale(Math.sin(angle * Math.PI / 180)));
	}

	public CoordinateVector(CoordinateLength x, CoordinateLength y) {
		unit = x.getUnit();
		this.x = x;
		this.y = y.withUnit(unit);
	}

	public CoordinateLength length() {
		double xVal = x.getOriginalValue();
		double yVal = y.getOriginalValue();
		return new CoordinateLength(unit, Math.sqrt(xVal * xVal + yVal * yVal));
	}

	public CoordinateVector normalize() {
		return scale(1 / length().getOriginalValue());
	}

	public CoordinateVector scale(double factor) {
		return new CoordinateVector(x.scale(factor), y.scale(factor));
	}

	public CoordinateVector scale(CoordinateLength factor) {
		return scale(factor.getValue(unit));
	}

	/**
	 * Return the vector rotated 90 degrees counter clockwise
	 */
	public CoordinateVector normal() {
		return new CoordinateVector(y.scale(-1), x);
	}

	public CoordinateVector minus(CoordinateVector other) {
		return of(x.minus(other.x), y.minus(other.y));
	}

	public CoordinateVector negate() {
		return scale(-1);
	}

	public CoordinateVector plus(CoordinateVector other) {
		return of(x.plus(other.x), y.plus(other.y));
	}

	/**
	 * Angle in degrees between the vectors as measured in a counterclockwise
	 * direction from this vector to other
	 */
	public double angleTo(CoordinateVector other) {
		double result = 180 / Math.PI
				* Math.atan2(x.getValue(unit) * other.y.getValue(unit) - y.getValue(unit) * other.x.getValue(unit),
						x.getValue(unit) * other.x.getValue(unit) + y.getValue(unit) * other.y.getValue(unit));
		if (result < 0)
			result += 360;
		return result;
	}

	/**
	 * Angle in degrees between (1,0) and this vector measured in a counterclockwise
	 * direction
	 */
	public double angle() {
		double result = 180 / Math.PI * Math.atan2(y.getValue(unit), x.getValue(unit));
		if (result < 0)
			result += 360;
		return result;
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

	public CoordinateVector minus(CoordinateLength dx, CoordinateLength dy) {
		return minus(CoordinateVector.of(dx, dy));
	}

	public CoordinateVector minusX(CoordinateLength dx) {
		return minus(dx, CoordinateLength.of(unit, 0));
	}

	public CoordinateVector minusY(CoordinateLength dy) {
		return minus(CoordinateLength.of(unit, 0), dy);
	}

	public CoordinateVector plus(CoordinateLength dx, CoordinateLength dy) {
		return plus(CoordinateVector.of(dx, dy));
	}

	public CoordinateVector plusX(CoordinateLength dx) {
		return plus(dx, CoordinateLength.of(unit, 0));
	}

	public CoordinateVector plusY(CoordinateLength dy) {
		return plus(CoordinateLength.of(unit, 0), dy);
	}

	public double dotProduct(CoordinateVector other) {
		return x.getValue(unit) * other.x.getValue(unit) + y.getValue(unit) * other.y.getValue(unit);
	}

	/**
	 * Rotate vector by angle counter clockwise in degrees
	 */
	public CoordinateVector rotate(double angle) {
		double a = angle / 180 * Math.PI;
		double x = this.x.getValue(unit);
		double y = this.y.getValue(unit);
		return of(unit, Math.cos(a) * x - Math.sin(a) * y, Math.sin(a) * x + Math.cos(a) * y);
	}

}
