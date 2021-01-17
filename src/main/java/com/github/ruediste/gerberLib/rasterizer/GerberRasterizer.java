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
	private SparseBufferedImage currentImage;
	private double offsetXMM;
	private double offsetYMM;
	private double pointsPerMM;
	private Graphics2D currentImageGraphics;

	public GerberRasterizer(WarningCollector warningCollector, double widthMM, double heightMM, double offsetXMM,
			double offsetYMM, double pointsPerMM) {
		super(warningCollector);
		this.warningCollector = warningCollector;
		this.offsetXMM = offsetXMM;
		this.offsetYMM = offsetYMM;
		this.pointsPerMM = pointsPerMM;
		image = new BufferedImage((int) (widthMM * pointsPerMM), (int) (heightMM * pointsPerMM),
				BufferedImage.TYPE_BYTE_BINARY);
		g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		setImageTransform(g);
	}

	private void setImageTransform(Graphics2D g) {
		g.transform(AffineTransform.getTranslateInstance(0, image.getHeight()));
		g.transform(AffineTransform.getScaleInstance(pointsPerMM, -pointsPerMM));
		g.transform(AffineTransform.getTranslateInstance(offsetXMM, offsetYMM));
	}

	@Override
	public void beginObject(InputPosition pos) {
		currentImage = new SparseBufferedImage(image.getWidth(), image.getHeight(), 256);
		currentImageGraphics = currentImage.image.createGraphics();
		setImageTransform(currentImageGraphics);
	}

	@Override
	public void endPath(InputPosition pos, Exposure exposure) {

		switch (exposure) {
		case OFF:
			currentImageGraphics.setColor(Color.BLACK);
			break;
		case ON:
			currentImageGraphics.setColor(Color.WHITE);
			break;
		default:
			throw new UnsupportedOperationException();
		}

		currentImageGraphics.fill(currentPath);
		currentPath = null;
	}

	@Override
	public void endObject(InputPosition pos, Polarity polarity) {
		currentImage.multiplyTo(image, polarity);
		currentImage = null;
	}

	public void save(File file) {
		try {
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BufferedImage compareTo(File outFile) {
		try {
			BufferedImage reference = ImageIO.read(outFile);
			int maxWidth = Math.max(image.getWidth(), reference.getWidth());
			int maxHeight = Math.max(image.getHeight(), reference.getHeight());
			BufferedImage diff = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
			boolean diffFound = false;
			for (int x = 0; x < maxWidth; x++) {
				for (int y = 0; y < maxHeight; y++) {
					if (x >= image.getWidth() || x >= reference.getWidth() || y >= image.getHeight()
							|| y >= reference.getHeight()) {
						diff.setRGB(x, y, 0xff);
						diffFound = true;
						continue;
					}

					int imageRgb = image.getRGB(x, y);
					if (imageRgb != reference.getRGB(x, y)) {
						diffFound = true;
						if (imageRgb == 0) {
							diff.setRGB(x, y, 0xff0000);
						} else
							diff.setRGB(x, y, 0x00ff00);
					} else
						diff.setRGB(x, y, imageRgb);

				}
			}
			if (diffFound)
				return diff;
			else
				return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
