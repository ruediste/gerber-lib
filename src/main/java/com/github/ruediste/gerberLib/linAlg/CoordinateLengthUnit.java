package com.github.ruediste.gerberLib.linAlg;

public enum CoordinateLengthUnit {
	MM {
		@Override
		double convertTo(CoordinateLengthUnit unit, double value) {
			if (unit == MM)
				return value;
			else if (unit == IN)
				return value / 25.4;
			else
				throw new UnsupportedOperationException();
		}
	},
	IN {
		@Override
		double convertTo(CoordinateLengthUnit unit, double value) {
			if (unit == MM)
				return value * 25.4;
			else if (unit == IN)
				return value;
			else
				throw new UnsupportedOperationException();
		}
	};

	abstract double convertTo(CoordinateLengthUnit unit, double value);
}
