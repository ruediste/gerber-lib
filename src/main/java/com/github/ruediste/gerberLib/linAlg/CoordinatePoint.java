package com.github.ruediste.gerberLib.linAlg;

public class CoordinatePoint {

	public final double x;
	public final double y;

	public static CoordinatePoint of(double x, double y) {
		return new CoordinatePoint(x, y);
	}

	public CoordinatePoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public CoordinateVector vectorTo(CoordinatePoint target) {
		return CoordinateVector.of(target.x - x, target.y - y);
	}

	public CoordinatePoint plus(CoordinateVector v) {
		return of(x + v.x, y + v.y);
	}

	public CoordinatePoint minus(CoordinateVector v) {
		return of(x - v.x, y - v.y);
	}

	public CoordinatePoint plus(double x, double y) {
		return of(this.x + x, this.y + y);
	}

	public CoordinatePoint plusX(double x) {
		return of(this.x + x, this.y);
	}

	public CoordinatePoint plusY(double y) {
		return of(this.x, this.y + y);
	}

	public CoordinatePoint minus(double x, double y) {
		return of(this.x - x, this.y - y);
	}

	public CoordinatePoint minusX(double x) {
		return of(this.x - x, this.y);
	}

	public CoordinatePoint minusY(double y) {
		return of(this.x, this.y - y);
	}

	public CoordinatePoint projectPointToLine(CoordinatePoint point, CoordinateVector direction) {
		return this.plus(direction.scale(vectorTo(point).dotProduct(direction) / direction.dotProduct(direction)));
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

	/**
	 * Rotate point around origin by angle counter clockwise in degrees
	 */
	public CoordinatePoint rotate(double angle) {
		double a = angle / 180 * Math.PI;
		return of(Math.cos(a) * x - Math.sin(a) * y, Math.sin(a) * x + Math.cos(a) * y);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Double.hashCode(x);
		result = prime * result + Double.hashCode(y);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CoordinatePoint other = (CoordinatePoint) obj;
		return x == other.x && y == other.y;
	}

}
