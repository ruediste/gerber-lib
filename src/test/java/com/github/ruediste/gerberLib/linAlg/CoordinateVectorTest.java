package com.github.ruediste.gerberLib.linAlg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CoordinateVectorTest {

	@Test
	public void testAngle() throws Exception {
		assertEquals(90, CoordinateVector.of(0, 1).angle());
		assertEquals(0, CoordinateVector.of(1, 0).angle());
		assertEquals(45, CoordinateVector.of(1, 1).angle());
		assertEquals(180, CoordinateVector.of(-1, 0).angle());
		assertEquals(270, CoordinateVector.of(0, -1).angle());
		assertEquals(225, CoordinateVector.of(-1, -1).angle());
	}
}
