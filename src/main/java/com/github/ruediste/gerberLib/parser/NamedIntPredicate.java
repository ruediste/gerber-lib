package com.github.ruediste.gerberLib.parser;

import java.util.function.IntPredicate;

public interface NamedIntPredicate extends IntPredicate {
	String name();

	static NamedIntPredicate of(String name, IntPredicate delegate) {
		return new NamedIntPredicate() {

			@Override
			public boolean test(int value) {
				return delegate.test(value);
			}

			@Override
			public String name() {
				return name;
			}
		};
	}
}