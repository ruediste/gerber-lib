package com.github.ruediste.gerberLib.parser;

import java.util.HashSet;
import java.util.Set;

public class ParsingContext<T extends ParsingState<T>> {

	public T state;
	final public String input;

	final private ParseException singletonParseException;

	private InputPosition latestInputPosition;
	private Set<String> latestExpected;

	public int backtrackingLimit = -1;

	public ParsingContext(String input, T initialState) {
		this.input = input;
		this.state = initialState;
		singletonParseException = new ParseException(
				Set.of("use ParsingContext.throwNiceParseException() for a proper parse error"), new InputPosition(),
				input);
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

	public void expected(Set<String> expected) {
		expected(expected, state.pos);
	}

	public ParseException throwException(Set<String> expected, InputPosition pos) {
		expected(expected, pos);
		throw singletonParseException;
	}

	public void expected(Set<String> expected, InputPosition pos) {
		if (latestInputPosition == null || pos.isAfter(latestInputPosition)) {
			latestInputPosition = pos.copy();
			latestExpected = new HashSet<>(expected);
			return;
		}
		if (pos.isBefore(latestInputPosition))
			return;
		latestExpected.addAll(expected);
	}

	public ParseException throwException(String expected) {
		return throwException(expected, state.pos);
	}

	public ParseException throwException() {
		throw singletonParseException;
	}

	public void expected(String expected) {
		expected(expected, state.pos);
	}

	public ParseException throwException(String expected, InputPosition pos) {
		return throwException(Set.of(expected), pos);
	}

	public void expected(String expected, InputPosition pos) {
		expected(Set.of(expected), pos);
	}

	public InputPosition copyPos() {
		return state.pos.copy();
	}

	public void limitBacktracking() {
		backtrackingLimit = state.pos.inputIndex;
	}

	public void throwNiceParseException(Runnable r) {
		try {
			r.run();
		} catch (ParseException e) {
			if (latestInputPosition == null)
				new RuntimeException("Parse exception thrown without calling ParsingContext.throwException()");
			throw new ParseException(latestExpected, latestInputPosition, input);
		}
	}
}
