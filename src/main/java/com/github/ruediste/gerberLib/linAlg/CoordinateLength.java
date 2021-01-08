package com.github.ruediste.gerberLib.linAlg;

public class CoordinateLength implements Comparable<CoordinateLength> {
	public static final CoordinateLength ZERO = of(CoordinateLengthUnit.MM, 0);
	private final double value;
	private final CoordinateLengthUnit unit;

	public CoordinateLength(CoordinateLengthUnit unit, double value) {
		this.unit = unit;
		this.value = value;
	}

	public static CoordinateLength of(CoordinateLengthUnit unit, double value) {
		return new CoordinateLength(unit, value);
	}

	public double getValue(CoordinateLengthUnit unit) {
		return this.unit.convertTo(unit, value);
	}

	public double getOriginalValue() {
		return value;
	}

	public CoordinateLengthUnit getUnit() {
		return unit;
	}

	public CoordinateLength scale(double factor) {
		return new CoordinateLength(unit, value * factor);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		long temp;
		temp = Double.doubleToLongBits(value);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		CoordinateLength other = (CoordinateLength) obj;
		if (unit != other.unit)
			return false;
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value + " " + unit;
	}

	public CoordinateLength minus(CoordinateLength other) {
		return of(unit, value - other.getValue(unit));
	}

	public CoordinateLength plus(CoordinateLength other) {
		return of(unit, value + other.getValue(unit));
	}

	public CoordinateLength withUnit(CoordinateLengthUnit unit) {
		if (this.unit == unit)
			return this;
		return CoordinateLength.of(unit, getValue(unit));
	}

	@Override
	public int compareTo(CoordinateLength other) {
		return Double.compare(value, other.getValue(unit));
	}

}
