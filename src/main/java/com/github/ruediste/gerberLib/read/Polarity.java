package com.github.ruediste.gerberLib.read;

public enum Polarity {
	CLEAR {
		@Override
		public Polarity negate() {
			return DARK;
		}
	},
	DARK {
		@Override
		public Polarity negate() {
			return CLEAR;
		}
	};

	public abstract Polarity negate();

	public Polarity negate(boolean negate) {
		if (negate)
			return negate();
		else
			return this;
	}
}
