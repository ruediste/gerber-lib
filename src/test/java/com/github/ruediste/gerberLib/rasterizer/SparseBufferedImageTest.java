package com.github.ruediste.gerberLib.rasterizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.ruediste.gerberLib.read.Polarity;

public class SparseBufferedImageTest {

	@Test
	public void test() throws FileNotFoundException, IOException {
		BufferedImage target = new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
		SparseBufferedImage sparse = new SparseBufferedImage(100, 100, 10);
		{
			Graphics2D g = target.createGraphics();
			g.fillRect(0, 0, target.getWidth(), target.getHeight());
			g.setColor(Color.BLACK);
			g.fill(new Arc2D.Double(30, 30, 50, 50, 0, -90, Arc2D.PIE));
		}
		{
			Graphics2D g = sparse.image.createGraphics();
			g.setColor(Color.BLACK);
			g.fill(new Arc2D.Double(10, 10, 50, 50, 0, 200, Arc2D.PIE));
		}

		sparse.multiplyTo(target, Polarity.DARK);
//		ImageIO.write(target, "png", new FileOutputStream("test.png"));
	}
}
