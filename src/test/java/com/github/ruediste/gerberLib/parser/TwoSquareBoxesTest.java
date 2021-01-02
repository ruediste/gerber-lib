package com.github.ruediste.gerberLib.parser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class TwoSquareBoxesTest {

	@Test
	public void test() {
		GerberParsingEventHandler handler = mock(GerberParsingEventHandler.class);
		GerberParser parser = new GerberParser(handler,
				TestUtils.readResource("/examples20201015/2-13-1_Two_square_boxes.gbr"));
		parser.file();
		InOrder o = inOrder(handler);
		o.verify(handler).comment(any(), eq(" Ucamco ex. 1: Two square boxes"));
		o.verify(handler).coordinateFormatSpecification(any(), eq(new GerberCoordinateFormatSpecification(2, 6, 2, 6)));
		o.verify(handler).unit(any(), eq("MM"));
		o.verify(handler).fileAttribute(any(), eq(".Part"), eq(List.of("Other", "example")));
		o.verify(handler).loadPolarity(any(), eq("D"));
		o.verify(handler).apertureDefinition(any(), eq("D10"), eq("C"), eq(List.of("0.010")));
		o.verify(handler).setCurrentAperture(any(), eq("D10"));
		o.verify(handler).moveOperation(any(), eq("0"), eq("0"));
		o.verify(handler).linearIntrepolation(any());
		o.verify(handler).interpolateOperation(any(), eq("5000000"), eq("0"), eq(null));
		o.verify(handler).interpolateOperation(any(), eq(null), eq("5000000"), eq(null));
		o.verify(handler).interpolateOperation(any(), eq("0"), eq(null), eq(null));
		o.verify(handler).interpolateOperation(any(), eq(null), eq("0"), eq(null));
		o.verify(handler).moveOperation(any(), eq("6000000"), eq(null));

		o.verify(handler).interpolateOperation(any(), eq("11000000"), eq(null), eq(null));
		o.verify(handler).interpolateOperation(any(), eq(null), eq("5000000"), eq(null));
		o.verify(handler).interpolateOperation(any(), eq("6000000"), eq(null), eq(null));
		o.verify(handler).interpolateOperation(any(), eq(null), eq("0"), eq(null));

		o.verify(handler).endOfFile(any());
		verifyNoMoreInteractions(handler);
	}
}
