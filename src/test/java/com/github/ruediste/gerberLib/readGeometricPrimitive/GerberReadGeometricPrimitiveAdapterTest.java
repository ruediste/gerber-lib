package com.github.ruediste.gerberLib.readGeometricPrimitive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class GerberReadGeometricPrimitiveAdapterTest {

	@Test
	public void testAngle() throws Exception {
		GerberReadGeometricPrimitiveAdapter a = new GerberReadGeometricPrimitiveAdapter(null, null);
		assertEquals(90, a.angle(0, 90, false));
		assertEquals(90, a.angle(315, 45, false));
		assertEquals(270, a.angle(90, 0, false));
		assertEquals(270, a.angle(45, 315, false));

		assertEquals(-270, a.angle(0, 90, true));
		assertEquals(-270, a.angle(315, 45, true));
		assertEquals(-90, a.angle(90, 0, true));
		assertEquals(-90, a.angle(45, 315, true));

	}

}
