package com.github.ruediste.gerberLib;

import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.github.ruediste.gerberLib.linAlg.CoordinateLength;
import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.read.Polarity;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveEventHandler;

public class GerberBoundingBoxCollector implements GerberReadGeometricPrimitiveEventHandler {
	public Area fullArea = new Area();
	private Area currentArea;
	private Path2D currentPath;

	@Override
	public void beginObject(InputPosition pos, Polarity polarity) {
		currentArea = new Area();
	}

	@Override
	public void beginPath(InputPosition pos, Exposure exposure) {
		currentPath = new Path2D.Double(Path2D.WIND_EVEN_ODD);
	}

	@Override
	public void addLine(InputPosition pos, CoordinatePoint p1, CoordinatePoint p2) {
		currentPath.append(new Line2D.Double(p(p1), p(p2)), true);
	}

	private Point2D p(CoordinatePoint p) {
		return new Point2D.Double(l(p.x), l(p.y));
	}

	private double l(CoordinateLength x) {
		return x.getValue(CoordinateLengthUnit.MM);
	}

	@Override
	public void addArc(InputPosition pos, CoordinatePoint point, CoordinateLength w, CoordinateLength h, double angSt,
			double angExt) {
		var p = p(point);
		currentPath.append(new Arc2D.Double(p.getX(), p.getY(), l(w), l(h), angSt, angExt, Arc2D.OPEN), true);
	}

	@Override
	public void endPath(InputPosition pos, Exposure exposure) {

		switch (exposure) {
		case OFF:
			currentArea.subtract(new Area(currentPath));
			break;
		case ON:
			currentArea.add(new Area(currentPath));
			break;
		default:
			throw new UnsupportedOperationException();
		}

		currentPath = null;
	}

	@Override
	public void endObject(InputPosition pos, Polarity polarity) {
		fullArea.add(currentArea);
		currentArea = null;
	}

	public Rectangle2D getBounds() {
		return fullArea.getBounds2D();
	}

}
