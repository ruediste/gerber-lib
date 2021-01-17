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

	public boolean isEqualTo(File outFile) {
		try {
			BufferedImage reference = ImageIO.read(outFile);
			if (image.getWidth() != reference.getWidth() || image.getHeight() != reference.getHeight()) {
				return false;
			}
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					if (image.getRGB(x, y) != reference.getRGB(x, y))
						return false;
				}
			}
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
