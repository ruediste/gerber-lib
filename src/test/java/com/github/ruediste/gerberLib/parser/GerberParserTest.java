package com.github.ruediste.gerberLib.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GerberParserTest {

	GerberParsingEventHandler handler;

	private GerberParser parser(String input) {

		return new GerberParser(handler, input);
	}

	@BeforeEach
	public void before() {
		handler = mock(GerberParsingEventHandler.class);
	}

	@Test
	public void testString() {
		assertEquals("abc", parser("abc").string());
		assertEquals("abc", parser("abc*").string());
		assertEquals("abc", parser("abc*de").string());
		assertEquals("abc", parser("a\nbc").string());
	}

	@Test
	public void testComment_G04() throws Exception {
		parser("G04 Hello World*").comment_G04();
		verify(handler).comment(any(), eq(" Hello World"));
	}
}
