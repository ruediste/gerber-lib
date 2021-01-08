package com.github.ruediste.gerberLib.readGeometricPrimitive;

import com.github.ruediste.gerberLib.linAlg.CoordinateLength;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.read.Polarity;

public interface GerberReadGeometricPrimitiveEventHandler {

	enum Exposure {
		/**
		 * Add a path the the current object
		 */
		ON,

		/**
		 * Remove erase areas previously created in same Object
		 */
		OFF,
	}

	void beginObject(InputPosition pos, Polarity polarity);

	void beginPath(InputPosition pos, Exposure exposure);

	void addLine(InputPosition pos, CoordinatePoint p1, CoordinatePoint p2);

	/**
	 * @param p      The coordinate of the lower-left corner of the arc.
	 * @param w      The overall width of the full ellipse of which this arc is a
	 *               partial section.
	 * @param h      The overall height of the full ellipse of which this arc is a
	 *               partial section.
	 * @param angSt  The starting angle of the arc in degrees.
	 * @param angExt The angular extent of the arc in degrees.
	 */
	void addArc(InputPosition pos, CoordinatePoint p, CoordinateLength w, CoordinateLength h, double angSt,
			double angExt);

	void endPath(InputPosition pos, Exposure exposure);

	void endObject(InputPosition pos, Polarity polarity);

}
