package com.github.ruediste.gerberLib.read;

import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.parser.GerberCoordinateFormatSpecification;
import com.github.ruediste.gerberLib.parser.InterpolationMode;

public class GerberGraphicsState {
	public GerberCoordinateFormatSpecification coordinateFormat;
	public CoordinateLengthUnit unit;
	public Double currentX;
	public Double currentY;
	public ApertureDefinition currentAperture;
	public InterpolationMode interpolationMode;
	public QuadrantMode quadrantMode;
	public Polarity polarity;

	public CoordinatePoint current() {
		return CoordinatePoint.of(currentX, currentY);
	}
}
