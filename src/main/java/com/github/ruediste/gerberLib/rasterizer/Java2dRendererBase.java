package com.github.ruediste.gerberLib.rasterizer;

import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveEventHandler;

public abstract class Java2dRendererBase implements GerberReadGeometricPrimitiveEventHandler {

	private static final boolean print = true;

	protected WarningCollector warningCollector;

	public Java2dRendererBase(WarningCollector warningCollector) {
		this.warningCollector = warningCollector;
	}

	protected Area currentArea;
	protected Path2D currentPath;

	@Override
	public void beginObject(InputPosition pos) {
		currentArea = new Area();
	}

	@Override
	public void beginPath(InputPosition pos) {
		currentPath = new Path2D.Double(Path2D.WIND_EVEN_ODD);
	}

	@Override
	public void addLine(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint p1,
			CoordinatePoint p2) {
		if (print)
			System.out.println(pos + ": line " + p1 + "->" + p2 + " " + transformation);
		currentPath.append(new Line2D.Double(new Point2D.Double(p1.x, p1.y), new Point2D.Double(p2.x, p2.y))
				.getPathIterator(transformation.inner), true);
	}

	@Override
	public void addArc(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint p, double w,
			double h, double angSt, double angExt) {
		if (print)
			System.out.println(
					pos + ": arc " + p + "(" + w + "," + h + ")[" + angSt + "," + angExt + "] " + transformation);
		currentPath.append(
				new Arc2D.Double(p.x, p.y, w, h, -angSt, -angExt, Arc2D.OPEN).getPathIterator(transformation.inner),
				true);
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

}
