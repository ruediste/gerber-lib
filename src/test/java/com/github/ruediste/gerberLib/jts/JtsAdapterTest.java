package com.github.ruediste.gerberLib.jts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.parser.GerberParser;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveAdapter;

public class JtsAdapterTest {

	@Test
	public void test() throws IOException {
		WarningCollector warningCollector = new WarningCollector();

		String gbrContent = new String(

				Files.readAllBytes(Paths.get("samples/kicadX3/dvk-mx8m-bsb-B_Cu.gbr")),
//				Files.readAllBytes(Paths.get("samples/examples20201015/2-13-2_Polarities_and_Apertures.gbr")),

				StandardCharsets.UTF_8);
		JtsAdapter jtsAdapter = new JtsAdapter();
		new GerberParser(new GerberReadGraphicsAdapter(warningCollector,
				new GerberReadGeometricPrimitiveAdapter(warningCollector, jtsAdapter)), gbrContent).file();

		var buffers = new ArrayList<Geometry>();
		Geometry image = jtsAdapter.image();

		{
			double bufferSize = -0.04;
			Geometry buffer = image;
			while (true) {
				buffer = buffer.buffer(bufferSize);
				if (buffer.isEmpty())
					break;
				buffers.add(buffer);
				if (bufferSize >= -0.25)
					bufferSize *= 2;
			}
		}

		ShapeWriter writer = new ShapeWriter();
		Rectangle2D bounds = writer.toShape(image).getBounds2D();
		double pointsPerMM = 100;
		BufferedImage target = new BufferedImage((int) (bounds.getWidth() * pointsPerMM),
				(int) (bounds.getHeight() * pointsPerMM), BufferedImage.TYPE_INT_RGB);
		var g = target.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, target.getWidth(), target.getHeight());
		g.transform(AffineTransform.getTranslateInstance(0, target.getHeight()));
		g.transform(AffineTransform.getScaleInstance(pointsPerMM, -pointsPerMM));
		g.transform(AffineTransform.getTranslateInstance(-bounds.getMinX(), -bounds.getMinY()));

		g.setColor(Color.BLACK);
		g.fill(writer.toShape(image));

		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(0.01f));
		buffers.forEach(b -> g.draw(writer.toShape(b)));

		System.out.println(bounds);
		ImageIO.write(target, "png", new FileOutputStream("test.png"));

		try (var out = new FileWriter("test.nc", StandardCharsets.UTF_8)) {

			out.append("G0 F1000\nG1 F100\n");
			MoveGenerator moveGenerator = new MoveGenerator(warningCollector, new MoveHandler() {

				@Override
				public void moveTo(Coordinate coordinate) {
					try {
						out.append(String.format("G0 X%.2f Y%.2f\n", coordinate.x, coordinate.y));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public void lineTo(Coordinate coordinate) {
					try {
						out.append(String.format("G1 X%.2f Y%.2f\n", coordinate.x, coordinate.y));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});

			buffers.forEach(moveGenerator::add);
			moveGenerator.generateMoves(new Coordinate(0, 0));
		}

		warningCollector.warnings.forEach(x -> System.out.println(x.pos + " " + x.message));

	}
}
