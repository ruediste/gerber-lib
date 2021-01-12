package com.github.ruediste.gerberLib.read;

import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.linAlg.CoordinateVector;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.parser.InterpolationMode;

public class InterpolateParameter {

	public InputPosition pos;
	public CoordinatePoint current;
	public CoordinatePoint target;
	public CoordinateVector ij;

	public CoordinateTransformation transformation;

	public ApertureDefinition currentAperture;
	public InterpolationMode interpolationMode;
	public QuadrantMode quadrantMode;
	public Polarity polarity;

	public InterpolateParameter(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint current,
			CoordinatePoint target, CoordinateVector ij, ApertureDefinition currentAperture,
			InterpolationMode interpolationMode, QuadrantMode quadrantMode, Polarity polarity) {
		this.pos = pos;
		this.transformation = transformation;
		this.current = current;
		this.target = target;
		this.ij = ij;
		this.currentAperture = currentAperture;
		this.interpolationMode = interpolationMode;
		this.quadrantMode = quadrantMode;
		this.polarity = polarity;
	}

}