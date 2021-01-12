package com.github.ruediste.gerberLib.read;

import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.Deque;

import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.parser.GerberCoordinateFormatSpecification;
import com.github.ruediste.gerberLib.parser.InterpolationMode;

class GerberGraphicsState {
	public GerberCoordinateFormatSpecification coordinateFormat;
	public CoordinateLengthUnit unit;
	public Double currentX;
	public Double currentY;

	public ApertureDefinition currentAperture;
	public InterpolationMode interpolationMode = InterpolationMode.LINEAR;
	public QuadrantMode quadrantMode;
	public Polarity polarity = Polarity.DARK;

	public String mirroring = "N";
	public double rotation = 0;
	public double scaling = 1;

	public CoordinatePoint current() {
		return CoordinatePoint.of(currentX, currentY);
	}

	Deque<CoordinateTransformation> blockTransformations = new ArrayDeque<>();
	{
		blockTransformations.push(new CoordinateTransformation());
	}
	CoordinateTransformation apertureTransformation = new CoordinateTransformation();

	public void updateApertureTransformation() {
		double mirrorX = 1;
		double mirrorY = 1;
		switch (mirroring) {
		case "N":
			// NOP
			break;
		case "X":
			mirrorX = -1;
			break;
		case "Y":
			mirrorY = -1;
			break;
		case "XY":
			mirrorX = -1;
			mirrorY = -1;
			break;
		default:
			throw new UnsupportedOperationException(mirroring);
		}

		AffineTransform t = AffineTransform.getRotateInstance(rotation * Math.PI / 180);
		t.scale(mirrorX * scaling, mirrorY * scaling);
		apertureTransformation = new CoordinateTransformation(t);
	}
}
