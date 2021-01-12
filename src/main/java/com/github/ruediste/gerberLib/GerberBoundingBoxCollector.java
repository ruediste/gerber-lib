package com.github.ruediste.gerberLib;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.rasterizer.Java2dRendererBase;
import com.github.ruediste.gerberLib.read.Polarity;

public class GerberBoundingBoxCollector extends Java2dRendererBase {

	public Area fullArea = new Area();

	public GerberBoundingBoxCollector(WarningCollector warningCollector) {
		super(warningCollector);
	}

	@Override
	public void endObject(InputPosition pos, Polarity polarity) {
		if (polarity == Polarity.DARK)
			fullArea.add(currentArea);
		else
			fullArea.subtract(currentArea);
		currentArea = null;
	}

	public Rectangle2D getBounds() {
		return fullArea.getBounds2D();
	}

}
