package com.github.ruediste.gerberLib.parser;

import java.awt.geom.Rectangle2D;
import java.io.File;

import org.junit.jupiter.api.Test;

import com.github.ruediste.gerberLib.GerberBoundingBoxCollector;
import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.rasterizer.GerberRasterizer;
import com.github.ruediste.gerberLib.read.GerberReadAdapter;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveAdapter;

public class DrawSamplesTest {

	private static String[] fileNames = new String[] { "2-13-1_Two_square_boxes.gbr",
			"2-13-2_Polarities_and_Apertures.gbr", "4-11-6_Block_with_different_orientations.gbr",
			"4-6-4_Nested_blocks.gbr", "6-1-6-2_A_drill_file.gbr", "sample_macro.gbr", "sample_macro_X1.gbr",
			"SMD_prim_20.gbr", "SMD_prim_20_X1.gbr", "SMD_prim_21.gbr", "SMD_prim_21_X1.gbr" };

	@Test
	public void drawSamples() {
		File dir = new File("sampleImages");
		for (String fileName : fileNames) {
			System.out.println("Rendering " + fileName);
			WarningCollector warningCollector = new WarningCollector();
			String gbrContent = TestUtils.readResource("/examples20201015/" + fileName);

			var boundsCollector = new GerberBoundingBoxCollector();
			new GerberParser(new GerberReadAdapter(warningCollector,
					new GerberReadGeometricPrimitiveAdapter(warningCollector, boundsCollector)), gbrContent).file();
			Rectangle2D bounds = boundsCollector.getBounds();

			warningCollector.warnings.clear();

			GerberRasterizer rasterizer = new GerberRasterizer(warningCollector, bounds.getWidth() + 2,
					bounds.getHeight() + 2, 1 - bounds.getMinX(), 1 - bounds.getMinY(), 100);
			new GerberParser(new GerberReadAdapter(warningCollector,
					new GerberReadGeometricPrimitiveAdapter(warningCollector, rasterizer)), gbrContent).file();

			if (!warningCollector.warnings.isEmpty()) {
				System.out.println("There were warnings:");
				for (var warning : warningCollector.warnings) {
					System.out.println(warning.pos + ": " + warning.message);
				}
				System.out.println("Review Image");
			}

			String outName = fileName.substring(0, fileName.length() - 3) + "png";
			rasterizer.save(new File(dir, outName));

		}
	}
}
