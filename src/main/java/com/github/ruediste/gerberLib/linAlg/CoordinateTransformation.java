package com.github.ruediste.gerberLib.linAlg;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class CoordinateTransformation {

	public AffineTransform inner = new AffineTransform();

	public CoordinateTransformation() {
	}

	public CoordinateTransformation(AffineTransform t) {
		inner = t;
	}

	public CoordinatePoint transform(CoordinatePoint p) {
		Point2D result = new Point2D.Double(p.x, p.y);
		inner.transform(result, result);
		return CoordinatePoint.of(result.getX(), result.getY());
	}

	public CoordinateTransformation copy() {
		return new CoordinateTransformation(new AffineTransform(inner));
	}

	public void translate(CoordinatePoint p) {
		inner.translate(p.x, p.y);
	}

	public void concatenate(CoordinateTransformation t) {
		inner.concatenate(t.inner);
	}

	@Override
	public String toString() {
		return inner.toString();
	}
}
