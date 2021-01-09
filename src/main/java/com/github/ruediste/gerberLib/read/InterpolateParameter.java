package com.github.ruediste.gerberLib.read;

import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateVector;
import com.github.ruediste.gerberLib.parser.InputPosition;

public class InterpolateParameter {

	public InputPosition pos;
	public GerberGraphicsState state;
	public CoordinatePoint target;
	public CoordinateVector ij;

	public InterpolateParameter(InputPosition pos, GerberGraphicsState state, CoordinatePoint target, CoordinateVector ij) {
		this.pos = pos;
		this.state = state;
		this.target = target;
		this.ij = ij;
	}
}