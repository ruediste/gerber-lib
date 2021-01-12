package com.github.ruediste.gerberLib.rasterizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.read.Polarity;

public class GerberRasterizer extends Java2dRendererBase {

	private Graphics2D g;
	public BufferedImage image;

	public GerberRasterizer(WarningCollector warningCollector, double widthMM, double heightMM, double offsetXMM,
			double offsetYMM, double pointsPerMM) {
		super(warningCollector);
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

}
