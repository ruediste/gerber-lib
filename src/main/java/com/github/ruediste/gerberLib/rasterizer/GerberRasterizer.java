package com.github.ruediste.gerberLib.rasterizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinateLength;
import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.read.Polarity;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveEventHandler;

public class GerberRasterizer implements GerberReadGeometricPrimitiveEventHandler {

	private Graphics2D g;
	public BufferedImage image;
	private int pointsPerMM;
	private WarningCollector warningCollector;
	private double offsetXMM;
	private double offsetYMM;

	public GerberRasterizer(WarningCollector warningCollector, double widthMM, double heightMM, double offsetXMM,
			double offsetYMM, int pointsPerMM) {
		this.pointsPerMM = pointsPerMM;
		this.offsetXMM = offsetXMM;
		this.offsetYMM = offsetYMM;
		this.warningCollector = warningCollector;
		image = new BufferedImage((int) (widthMM * pointsPerMM), (int) (heightMM * pointsPerMM),
				BufferedImage.TYPE_BYTE_BINARY);
		g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.transform(AffineTransform.getTranslateInstance(0, image.getHeight()));
		g.transform(AffineTransform.getScaleInstance(pointsPerMM, -pointsPerMM));
		g.transform(AffineTransform.getTranslateInstance(offsetXMM, offsetYMM));
	}

	Area currentArea;
	Path2D currentPath;

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
//		System.out.println(pos + ": line " + p1 + "->" + p2);
//		System.out.println(pos + ": line " + toImage(p1) + "->" + toImage(p2));
		currentPath.append(new Line2D.Double(new Point2D.Double(value(p1.x), value(p1.y)),
				new Point2D.Double(value(p2.x), value(p2.y))), true);
	}

	@Override
	public void addArc(InputPosition pos, CoordinatePoint p, CoordinateLength w, CoordinateLength h, double angSt,
			double angExt) {
//		System.out.println(pos + ": arc " + p + "(" + w + "," + h + ")[" + angSt + "," + angExt + "]");
		double iH = value(h);
//		System.out.println(pos + ": arc (" + toImageX(p.x) + "," + (toImageY(p.y) - iH) + ")(" + toImage(w) + "," + iH
//				+ ")[" + angSt + "," + angExt + "]");
		currentPath.append(new Arc2D.Double(value(p.x), value(p.y) - iH, value(w), iH, angSt, angExt, Arc2D.OPEN),
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

	@Override
	public void endObject(InputPosition pos, Polarity polarity) {
		if (polarity == Polarity.DARK)
			g.setColor(Color.BLACK);
		else
			g.setColor(Color.WHITE);
		g.fill(currentArea);
		currentArea = null;
	}

	public void save(File file) {
		try {
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private double value(CoordinateLength c) {
		return c.getValue(CoordinateLengthUnit.MM);
	}

}
