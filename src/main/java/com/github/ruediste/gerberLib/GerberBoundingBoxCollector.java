package com.github.ruediste.gerberLib;

import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.read.Polarity;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveEventHandler;

public class GerberBoundingBoxCollector implements GerberReadGeometricPrimitiveEventHandler {

	Rectangle2D bounds;

	public Rectangle2D getBounds() {
		return bounds;
	}

	@Override
	public void beginObject(InputPosition pos) {

	}

	@Override
	public void beginPath(InputPosition pos) {

	}

	@Override
	public void addLine(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint p1,
			CoordinatePoint p2) {
		var b = transformation.inner.createTransformedShape(new Line2D.Double(p1.x, p1.y, p2.x, p2.y)).getBounds2D();
		addBounds(b);
	}

	@Override
	public void addArc(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint p, double w,
			double h, double angSt, double angExt) {
		var b = transformation.inner
				.createTransformedShape(new Arc2D.Double(p.x, p.y, w, h, -angSt, -angExt, Arc2D.OPEN)).getBounds2D();
		addBounds(b);

	}

	private void addBounds(Rectangle2D b) {
		if (bounds == null)
			bounds = b;
		else
			bounds.add(b);
		if (Double.isNaN(bounds.getMinX()) || Double.isNaN(bounds.getMinY()) || Double.isNaN(bounds.getMaxX())
				|| Double.isNaN(bounds.getMaxY())) {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void endPath(InputPosition pos, Exposure exposure) {

	}

	@Override
	public void endObject(InputPosition pos, Polarity polarity) {

	}

}
