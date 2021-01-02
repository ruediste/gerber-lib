package com.github.ruediste.gerberLib.parser;

public class InputPosition {
	public int inputIndex = 0;
	public int lineStartIndex = 0;
	public int lineNr = 1;
	public int linePos = 1;

	public InputPosition copy() {
		InputPosition result = new InputPosition();
		result.inputIndex = inputIndex;
		result.lineStartIndex = lineStartIndex;
		result.lineNr = lineNr;
		result.linePos = linePos;
		return result;
	}

	public boolean isBefore(InputPosition other) {
		return inputIndex < other.inputIndex;
	}

	public boolean isAfter(InputPosition other) {
		return inputIndex > other.inputIndex;
	}

	public String lineWithMarker(String input) {
		StringBuilder line = new StringBuilder();
		StringBuilder marker = new StringBuilder();
		for (var idx = lineStartIndex; idx < input.length(); idx = input.offsetByCodePoints(idx, 1)) {
			int cp = input.codePointAt(idx);
			if (cp == '\n')
				break;
			line.appendCodePoint(cp);
			if (idx == inputIndex)
				marker.append('^');
			else if (idx < inputIndex)
				marker.append(' ');
		}
		return line + "\n" + marker;
	}

	@Override
	public String toString() {
		return "Line " + lineNr + ":" + linePos;
	}
}
