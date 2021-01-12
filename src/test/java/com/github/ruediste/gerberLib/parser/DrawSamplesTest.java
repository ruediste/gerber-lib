package com.github.ruediste.gerberLib.parser;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.github.ruediste.gerberLib.GerberBoundingBoxCollector;
import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.rasterizer.GerberRasterizer;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveAdapter;

public class DrawSamplesTest {

	@Test
	public void drawSamples() throws IOException {
		Files.walk(Paths.get("samples")).filter(x -> x.toString().endsWith(".gbr")).forEach(this::drawAndCompare);
	}

	void drawAndCompare(Path path) {
		try {
			System.out.println("Rendering " + path);
			WarningCollector warningCollector = new WarningCollector();

			String gbrContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

			var boundsCollector = new GerberBoundingBoxCollector(warningCollector);
			new GerberParser(new GerberReadGraphicsAdapter(warningCollector,
					new GerberReadGeometricPrimitiveAdapter(warningCollector, boundsCollector)), gbrContent).file();
			Rectangle2D bounds = boundsCollector.getBounds();

			warningCollector.warnings.clear();

			double widthMM = bounds.getWidth() + 2;
			double heightMM = bounds.getHeight() + 2;

			double maxD = Math.max(widthMM, heightMM);

			double pointsPerMM = Math.pow(10, Math.floor(Math.log10(10000 / maxD)));

			GerberRasterizer rasterizer = new GerberRasterizer(warningCollector, widthMM, heightMM,
					1 - bounds.getMinX(), 1 - bounds.getMinY(), pointsPerMM);
			new GerberParser(new GerberReadGraphicsAdapter(warningCollector,
					new GerberReadGeometricPrimitiveAdapter(warningCollector, rasterizer)), gbrContent).file();

			if (!warningCollector.warnings.isEmpty()) {
				System.out.println("There were warnings:");
				for (var warning : warningCollector.warnings) {
					System.out.println(warning.pos + ": " + warning.message);
				}
				System.out.println("Review Image");
			}

			String pathName = path.toString();
			String outName = pathName.substring(0, pathName.length() - 3) + "png";
			rasterizer.save(new File(outName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
