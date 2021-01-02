package com.github.ruediste.gerberLib.parser;

import java.util.HashSet;
import java.util.Set;

public class ParsingContext<T extends ParsingState<T>> {

	public T state;
	public String input;
	public ParseException latestException;

	public ParsingContext(String input, T initialState) {
		this.input = input;
		this.state = initialState;
	}

	public boolean isEof() {
		return state.pos.inputIndex >= input.length();
	}

	public int nextCp() {
		return state.nextCp(this);
	}

	public int peekCp() {
		if (isEof())
			throwException("any character");
		return input.codePointAt(state.pos.inputIndex);
	}

	public int peekCp(int offset) {
		int idx;
		try {
			idx = input.offsetByCodePoints(state.pos.inputIndex, offset);
			return input.codePointAt(idx);
		} catch (IndexOutOfBoundsException e) {
			throw throwException("any character");
		}
	}

	public ParseException throwException(Set<String> expected) {
		return throwException(expected, state.pos);
	}

	public ParseException throwException(Set<String> expected, InputPosition pos) {
		if (latestException == null) {
			latestException = new ParseException(expected, pos.copy(), input);
			throw latestException;
		}
		if (pos.isAfter(latestException.pos)) {
			latestException = new ParseException(expected, pos.copy(), input);
			throw latestException;
		}
		if (!expected.stream().allMatch(latestException.expected::contains)) {
			Set<String> set = new HashSet<String>(latestException.expected);
			set.addAll(expected);
			latestException = new ParseException(set, pos.copy(), input);
			throw latestException;
		}
		throw latestException;
	}

	public ParseException throwException(String expected) {
		return throwException(expected, state.pos);
	}

	public ParseException throwException(String expected, InputPosition pos) {
		return throwException(Set.of(expected), pos);
	}

	public InputPosition copyPos() {
		return state.pos.copy();
	}
}
