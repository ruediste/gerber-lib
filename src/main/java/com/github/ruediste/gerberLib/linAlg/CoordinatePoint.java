package com.github.ruediste.gerberLib.linAlg;

public class CoordinatePoint {

	public final CoordinateLength x;
	public final CoordinateLength y;

	public static CoordinatePoint of(CoordinateLength x, CoordinateLength y) {
		return new CoordinatePoint(x, y);
	}

	public static CoordinatePoint of(CoordinateLengthUnit unit, double x, double y) {
		return of(CoordinateLength.of(unit, x), CoordinateLength.of(unit, y));
	}

	public CoordinatePoint(CoordinateLength x, CoordinateLength y) {
		this.x = x;
		this.y = y;
	}

	public CoordinateVector vectorTo(CoordinatePoint target) {
		return CoordinateVector.of(target.x.minus(x), target.y.minus(y));
	}

	public CoordinatePoint plus(CoordinateVector v) {
		return of(x.plus(v.x), y.plus(v.y));
	}

	public CoordinatePoint minus(CoordinateVector v) {
		return of(x.minus(v.x), y.minus(v.y));
	}

	public CoordinatePoint plus(CoordinateLength x, CoordinateLength y) {
		return of(this.x.plus(x), this.y.plus(y));
	}

	public CoordinatePoint plusX(CoordinateLength x) {
		return of(this.x.plus(x), this.y);
	}

	public CoordinatePoint plusY(CoordinateLength y) {
		return of(this.x, this.y.plus(y));
	}

	public CoordinatePoint minus(CoordinateLength x, CoordinateLength y) {
		return of(this.x.minus(x), this.y.minus(y));
	}

	public CoordinatePoint minusX(CoordinateLength x) {
		return of(this.x.minus(x), this.y);
	}

	public CoordinatePoint minusY(CoordinateLength y) {
		return of(this.x, this.y.minus(y));
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
		CoordinateLengthUnit unit = x.getUnit();
		double x = this.x.getValue(unit);
		double y = this.y.getValue(unit);
		return of(unit, Math.cos(a) * x - Math.sin(a) * y, Math.sin(a) * x + Math.cos(a) * y);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		result = prime * result + ((y == null) ? 0 : y.hashCode());
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
		if (x == null) {
			if (other.x != null)
				return false;
		} else if (!x.equals(other.x))
			return false;
		if (y == null) {
			if (other.y != null)
				return false;
		} else if (!y.equals(other.y))
			return false;
		return true;
	}

}
