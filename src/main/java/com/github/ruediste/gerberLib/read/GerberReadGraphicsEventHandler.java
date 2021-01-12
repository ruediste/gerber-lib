package com.github.ruediste.gerberLib.read;

import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.parser.InputPosition;

public class GerberReadGraphicsEventHandler {

	public void interpolate(InterpolateParameter params) {
	}

	public void regionBegin(InputPosition pos) {

	}

	public void regionStartContour(InputPosition pos) {

	}

	public void regionInterpolate(InterpolateParameter params) {

	}

	public void regionEndContour(InputPosition pos) {

	}

	public void regionEnd(InputPosition pos, Polarity polarity) {

	}

	/**
	 * Flash the aperture at (0,0) and apply the transformation to it
	 */
	public void flash(InputPosition pos, CoordinateTransformation transformation, ApertureDefinition aperture,
			Polarity polarity) {
	}

}
