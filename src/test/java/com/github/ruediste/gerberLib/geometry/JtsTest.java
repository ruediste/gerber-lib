package com.github.ruediste.gerberLib.geometry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.awt.ShapeReader;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

public class JtsTest {

	@Test
	public void test() throws FileNotFoundException, IOException {
		GeometryFactory gf = new GeometryFactory();

		CoordinateArraySequence seq1 = new CoordinateArraySequence(new Coordinate[] { new Coordinate(0, 0),
				new Coordinate(1, 0), new Coordinate(0, 1), new Coordinate(0, 0) }, 2);
		Polygon poly1 = new Polygon(new LinearRing(seq1, gf), null, gf);
		CoordinateArraySequence seq2 = new CoordinateArraySequence(new Coordinate[] { new Coordinate(0.5, 0),
				new Coordinate(1.5, 0), new Coordinate(0.5, 1), new Coordinate(0.5, 0) }, 2);
		Polygon poly2 = new Polygon(new LinearRing(seq2, gf), null, gf);

		var geometries = new ArrayList<Geometry>();
		geometries.add(poly1);
		geometries.add(poly2);

		for (int x = 0; x < 100; x++)
			for (int y = 0; y < 100; y++) {
				geometries
						.add(ShapeReader.read(new Arc2D.Double(x, -(y + 1.1), 1.1, 1.1, 0, 270, Arc2D.PIE), 0.01, gf));
			}
		GeometryCollection coll = new GeometryCollection(geometries.toArray(new Geometry[] {}), gf);

		Geometry union = coll.union();

		var buffers = new ArrayList<Geometry>();
		Geometry buffer = union;
		while (true) {
			buffer = buffer.buffer(-0.1);
			if (buffer.isEmpty())
				break;
			buffers.add(buffer);
		}

		ShapeWriter writer = new ShapeWriter();
		Rectangle2D bounds = writer.toShape(union).getBounds2D();
		double pointsPerMM = 100;
		BufferedImage image = new BufferedImage((int) (bounds.getWidth() * pointsPerMM),
				(int) (bounds.getHeight() * pointsPerMM), BufferedImage.TYPE_INT_RGB);
		var g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		g.transform(AffineTransform.getTranslateInstance(0, image.getHeight()));
		g.transform(AffineTransform.getScaleInstance(pointsPerMM, -pointsPerMM));
		g.transform(AffineTransform.getTranslateInstance(-bounds.getMinX(), -bounds.getMinY()));

		g.setColor(Color.BLACK);
		g.fill(writer.toShape(union));

		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(0.01f));
		buffers.forEach(b -> g.draw(writer.toShape(b)));

		System.out.println(bounds);
		ImageIO.write(image, "png", new FileOutputStream("test.png"));
	}
}
