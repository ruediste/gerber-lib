package com.github.ruediste.gerberLib.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsEventHandler;

@ExtendWith(MockitoExtension.class)
public class BlockApertureTest {

	@Test
	public void test() {
		WarningCollector warningCollector = new WarningCollector();
		GerberReadGraphicsEventHandler handler = mock(GerberReadGraphicsEventHandler.class);
		new GerberParser(new GerberReadGraphicsAdapter(warningCollector, handler),
				"%FSLAX42Y42*%\n" + "%MOMM*%\n" + "%ADD10C,7.5*%\n" + "%ABD11*%\n" + "D10*\n" + "X200Y200D02*\n"
						+ "Y300D01*\n" + "%AB*%\n" + "D11*\n" + "X500Y500D03*\n" + "X1000Y500D03*\n" + "M02*").file();
		InOrder order = inOrder(handler);
		order.verify(handler).interpolate(argThat(p -> {
			assertEquals(CoordinatePoint.of(7, 8), p.target);
			return true;
		}));
	}
}
