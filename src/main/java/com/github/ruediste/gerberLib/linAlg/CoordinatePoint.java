package com.github.ruediste.gerberLib.linAlg;

public class CoordinatePoint {

	public final double x;
	public final double y;

	public static CoordinatePoint of(double x, double y) {
		return new CoordinatePoint(x, y);
	}

	public static CoordinatePoint ofAngular(double r, double angle) {
		return of(r * Math.cos(angle * Math.PI / 180), r * (Math.sin(angle * Math.PI / 180)));
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

	public static CoordinatePoint lineIntersection(CoordinatePoint p1, CoordinatePoint p2, CoordinatePoint p3,
			CoordinatePoint p4) {

		final double x1, y1, x2, y2, x3, y3, x4, y4;
		x1 = p1.x;
		y1 = p1.y;
		x2 = p2.x;
		y2 = p2.y;
		x3 = p3.x;
		y3 = p3.y;
		x4 = p4.x;
		y4 = p4.y;
		final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
		final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

		return CoordinatePoint.of(x, y);
	}
}
