package com.github.ruediste.gerberLib.readGeometricPrimitive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.read.Polarity;

public class CompoundGerberReadGeometricPrimitiveEventHandler implements GerberReadGeometricPrimitiveEventHandler {

	public List<GerberReadGeometricPrimitiveEventHandler> delegates = new ArrayList<>();

	public CompoundGerberReadGeometricPrimitiveEventHandler() {
	}

	public CompoundGerberReadGeometricPrimitiveEventHandler(GerberReadGeometricPrimitiveEventHandler... delegates) {
		this.delegates.addAll(Arrays.asList(delegates));
	}

	@Override
	public void beginObject(InputPosition pos) {
		delegates.forEach(x -> x.beginObject(pos));
	}

	@Override
	public void beginPath(InputPosition pos) {
		delegates.forEach(x -> x.beginPath(pos));
	}

	@Override
	public void addLine(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint p1,
			CoordinatePoint p2) {
		delegates.forEach(x -> x.addLine(pos, transformation, p1, p2));
	}

	@Override
	public void addArc(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint p, double w,
			double h, double angSt, double angExt) {
		delegates.forEach(x -> x.addArc(pos, transformation, p, w, h, angSt, angExt));
	}

	@Override
	public void endPath(InputPosition pos, Exposure exposure) {
		delegates.forEach(x -> x.endPath(pos, exposure));
	}

	@Override
	public void endObject(InputPosition pos, Polarity polarity) {
		delegates.forEach(x -> x.endObject(pos, polarity));
	}

}
