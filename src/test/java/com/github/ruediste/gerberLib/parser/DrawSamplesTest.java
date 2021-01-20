package com.github.ruediste.gerberLib.parser;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.github.ruediste.gerberLib.GerberBoundingBoxCollector;
import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.rasterizer.GerberRasterizer;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveAdapter;

public class DrawSamplesTest {

	@Test
	public void drawSamples() throws IOException {
		List<String> errors = Files.walk(Paths.get("samples"))

				.filter(x -> x.toString().endsWith(".gbr"))

				// .filter(x -> x.toString().contains("dvk-mx8m-bsb-B_Fab"))

				.flatMap(p -> drawAndCompare(p).stream()).collect(toList());
		if (!errors.isEmpty()) {
			errors.forEach(System.out::println);
			fail("There were errors. See stdout");
		} else {
			System.out.println("No differences found");
		}
	}

	List<String> drawAndCompare(Path path) {
		try {
			System.out.println("Rendering " + path);
			WarningCollector warningCollector = new WarningCollector();

			String gbrContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

			var boundsCollector = new GerberBoundingBoxCollector();
			new GerberParser(new GerberReadGraphicsAdapter(warningCollector,
					new GerberReadGeometricPrimitiveAdapter(warningCollector, boundsCollector)), gbrContent).file();
			Rectangle2D bounds = boundsCollector.getBounds();

			warningCollector.warnings.clear();

			double widthMM = bounds.getWidth();
			double heightMM = bounds.getHeight();

			double maxD = Math.max(widthMM, heightMM);

//			double pointsPerMM = Math.pow(10, Math.floor(Math.log10(10000 / maxD)));
			double pointsPerMM = 5000 / maxD;

			GerberRasterizer rasterizer = new GerberRasterizer(widthMM + 10 / pointsPerMM, heightMM + 10 / pointsPerMM,
					5 / pointsPerMM - bounds.getMinX(), 5 / pointsPerMM - bounds.getMinY(), pointsPerMM);
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
			File outFile = new File(pathName.substring(0, pathName.length() - 3) + "png");

			if (outFile.exists()) {
				BufferedImage diff = rasterizer.compareTo(outFile);
				if (diff == null) {
					System.out.println("Rendered image matches reference image");
				} else {
					System.out.println("Difference found");
					rasterizer.save(new File(pathName.substring(0, pathName.length() - 3) + "actual.png"));
					ImageIO.write(diff, "png", new File(pathName.substring(0, pathName.length() - 3) + "diff.png"));
					return List.of(path + ": Difference found");
				}
			} else {
				System.out.println("Saving " + outFile);
				rasterizer.save(outFile);
			}
			return List.of();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
