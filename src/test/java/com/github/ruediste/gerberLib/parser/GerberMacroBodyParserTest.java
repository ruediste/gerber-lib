package com.github.ruediste.gerberLib.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GerberMacroBodyParserTest {

	GerberParsingEventHandler handler;

	private GerberMacroBodyParser parser(String input) {
		return new GerberParser(handler, input).macroBodyParser;
	}

	@BeforeEach
	public void before() {
		handler = mock(GerberParsingEventHandler.class);
	}

	@Test
	public void testAssociativity() {
		assertEquals("(1)PLUS(2)", parseExp("1+2"));
		assertEquals("((1)PLUS(2))MINUS(1)", parseExp("1+2-1"));
		assertEquals("(1)MULTIPLY(2)", parseExp("1x2"));
		assertEquals("((1)MULTIPLY(2))DIVIDE(1)", parseExp("1x2/1"));

		assertEquals("(1)PLUS((2)MULTIPLY(3))", parseExp("1+2x3"));
		assertEquals("((1)MULTIPLY(2))PLUS(3)", parseExp("1x2+3"));

	}

	@Test
	public void testSampleExp() {
		assertEquals("(($1)DIVIDE(2))MINUS($3)", parseExp("$1/2-$3"));
	}

	private String parseExp(String exp) {
		return parser(exp).expression().toString();
	}
}
