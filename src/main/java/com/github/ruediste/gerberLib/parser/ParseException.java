package com.github.ruediste.gerberLib.parser;

import static java.util.stream.Collectors.joining;

import java.util.Set;

public class ParseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InputPosition pos;
	public Set<String> expected;

	public ParseException(Set<String> expected, InputPosition pos, String input) {
		super(pos + ": expected " + expected.stream().sorted().collect(joining(",")) + "\n"
				+ pos.lineWithMarker(input));
		this.pos = pos;
		this.expected = expected;
	}
}
