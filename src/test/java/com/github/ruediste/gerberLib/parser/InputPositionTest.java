package com.github.ruediste.gerberLib.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class InputPositionTest {

	String sampleText = "First Line\nSecond Line";

	@Test
	public void testLineWithMarker() throws Exception {
		InputPosition pos = new InputPosition();
		pos.lineNr = 2;
		pos.linePos = 6;
		pos.inputIndex = 16;
		pos.lineStartIndex = 11;

		assertEquals("\nSecond Line\n     ^", "\n" + pos.lineWithMarker(sampleText));
	}

}
