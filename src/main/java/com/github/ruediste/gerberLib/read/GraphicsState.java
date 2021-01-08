package com.github.ruediste.gerberLib.read;

import com.github.ruediste.gerberLib.linAlg.CoordinateLength;
import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateVector;
import com.github.ruediste.gerberLib.parser.GerberCoordinateFormatSpecification;
import com.github.ruediste.gerberLib.parser.InterpolationMode;

public class GraphicsState {
	public GerberCoordinateFormatSpecification coordinateFormat;
	public CoordinateLengthUnit unit;
	public CoordinateLength currentX;
	public CoordinateLength currentY;
	public ApertureDefinition currentAperture;
	public InterpolationMode interpolationMode;
	public QuadrantMode quadrantMode;
	public Polarity polarity;
	public CoordinatePoint current() {
		return CoordinatePoint.of(currentX,currentY);
	}
}
