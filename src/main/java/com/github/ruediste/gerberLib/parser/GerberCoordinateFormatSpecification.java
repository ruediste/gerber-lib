package com.github.ruediste.gerberLib.parser;

public class GerberCoordinateFormatSpecification {
	public int xIntegerDigits;
	public int xDecimalDigits;
	public int yIntegerDigits;
	public int yDecimalDigits;

	public GerberCoordinateFormatSpecification() {
	}

	public GerberCoordinateFormatSpecification(int xIntegerDigits, int xDecimalDigits, int yIntegerDigits,
			int yDecimalDigits) {
		this.xIntegerDigits = xIntegerDigits;
		this.xDecimalDigits = xDecimalDigits;
		this.yIntegerDigits = yIntegerDigits;
		this.yDecimalDigits = yDecimalDigits;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + xDecimalDigits;
		result = prime * result + xIntegerDigits;
		result = prime * result + yDecimalDigits;
		result = prime * result + yIntegerDigits;
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
		GerberCoordinateFormatSpecification other = (GerberCoordinateFormatSpecification) obj;
		if (xDecimalDigits != other.xDecimalDigits)
			return false;
		if (xIntegerDigits != other.xIntegerDigits)
			return false;
		if (yDecimalDigits != other.yDecimalDigits)
			return false;
		if (yIntegerDigits != other.yIntegerDigits)
			return false;
		return true;
	}

}