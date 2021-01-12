package com.github.ruediste.gerberLib.readGeometricPrimitive;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.linAlg.CoordinateVector;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitiveCenterLine;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitiveCircle;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitiveComment;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitiveMoire;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitiveOutline;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitivePolygon;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitiveThermal;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroPrimitiveVectorLine;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroStatementVisitor;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroVariableDefinitionStatement;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.parser.InterpolationMode;
import com.github.ruediste.gerberLib.read.ApertureDefinition;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsEventHandler;
import com.github.ruediste.gerberLib.read.InterpolateParameter;
import com.github.ruediste.gerberLib.read.MacroExpressionEvaluator;
import com.github.ruediste.gerberLib.read.Polarity;
import com.github.ruediste.gerberLib.read.QuadrantMode;
import com.github.ruediste.gerberLib.read.StandardApertureTemplate;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveEventHandler.Exposure;

public class GerberReadGeometricPrimitiveAdapter extends GerberReadGraphicsEventHandler {
	private GerberReadGeometricPrimitiveEventHandler handler;
	private WarningCollector warningCollector;

	public GerberReadGeometricPrimitiveAdapter(WarningCollector warningCollector,
			GerberReadGeometricPrimitiveEventHandler handler) {
		this.warningCollector = warningCollector;
		this.handler = handler;
	}

	@Override
	public void interpolate(InterpolateParameter params) {
		InputPosition pos = params.pos;
		handler.beginObject(pos);

		double width;
		ApertureDefinition aperture = params.currentAperture;
		if (aperture.standardTemplate == StandardApertureTemplate.C) {
			width = aperture.parameters.get(0);
		} else {
			warningCollector.add(pos,
					"Invalid aperture D" + aperture.nr + " for interpolation, only standard circle is allowed.");
			width = 1;
		}
		double width2 = width / 2;

		CoordinatePoint start = params.current;
		CoordinatePoint end = params.target;

		InterpolationMode interpolationMode = params.interpolationMode;
		if (interpolationMode == InterpolationMode.LINEAR) {
			var normal = start.vectorTo(end).normal().normalize();
			CoordinateVector offset = normal.scale(width2);
			var p1 = start.plus(offset);
			var p2 = end.plus(offset);
			var p3 = end.minus(offset);
			var p4 = start.minus(offset);

			handler.beginPath(pos);
			handler.addLine(pos, params.transformation, p1, p2);
			var corner = end.minus(CoordinateVector.of(width2, width2));
			handler.addArc(pos, params.transformation, corner, width, width, offset.angle(), -180);
			handler.addLine(pos, params.transformation, p3, p4);
			corner = start.minus(CoordinateVector.of(width2, width2));
			handler.addArc(pos, params.transformation, corner, width, width, offset.negate().angle(), -180);
			handler.endPath(pos, Exposure.ON);
		} else if (interpolationMode == InterpolationMode.CIRCULAR_CLOCKWISE
				|| interpolationMode == InterpolationMode.CIRCULAR_COUNTER_CLOCKWISE) {
			// circular interpolation
			boolean clockWise = interpolationMode == InterpolationMode.CIRCULAR_CLOCKWISE;
			QuadrantMode quadrantMode = params.quadrantMode;
			if (start.equals(end)) {
				switch (quadrantMode) {
				case MULTI: {
					var radius = params.ij.length();
					CoordinatePoint center = start.plus(params.ij);
					var radiusOuter = radius + width2;
					var radiusInner = radius - width2;
					handler.beginPath(pos);
					handler.addArc(pos, params.transformation, center.minus(radiusOuter, radiusOuter), radiusOuter * 2,
							radiusOuter * 2, 0, 360);
					handler.endPath(pos, Exposure.ON);

					handler.beginPath(pos);
					handler.addArc(pos, params.transformation, center.minus(radiusInner, radiusInner), radiusInner * 2,
							radiusInner * 2, 0, 360);
					handler.endPath(pos, Exposure.OFF);
				}
					break;
				case SINGLE: {
					handler.beginPath(pos);
					handler.addArc(pos, params.transformation, start.minus(width2, width2), width, width, 0,
							clockWise ? -360 : 360);
					handler.endPath(pos, Exposure.ON);
				}
					break;
				default:
					throw new UnsupportedOperationException();
				}
			} else {
				CoordinatePoint center = calculateCircleCenter(start, end, params.ij, quadrantMode, clockWise);
				double startAngle = center.vectorTo(start).angle();
				double endAngle = center.vectorTo(end).angle();
				var offsetStart = center.vectorTo(start).normalize().scale(width * 0.5);
				var offsetEnd = center.vectorTo(end).normalize().scale(width * 0.5);

				double radius = center.vectorTo(start).length();
				double radiusOuter = radius + offsetStart.length();
				double diameterOuter = radiusOuter * 2;
				double radiusInner = radius - offsetStart.length();
				double diameterInner = radiusInner * 2;

				handler.beginPath(pos);
				handler.addArc(pos, params.transformation, center.minus(radiusOuter, radiusOuter), diameterOuter,
						diameterOuter, startAngle, angle(startAngle, endAngle, clockWise));
				handler.addArc(pos, params.transformation, end.minus(width2, width2), width, width, offsetEnd.angle(),
						clockWise ? -180 : 180);
				handler.addArc(pos, params.transformation, center.minus(radiusInner, radiusInner), diameterInner,
						diameterInner, endAngle, angle(endAngle, startAngle, !clockWise));
				handler.addArc(pos, params.transformation, start.minus(width2, width2), width, width,
						offsetStart.angle(), clockWise ? 180 : -180);
				handler.endPath(pos, Exposure.ON);
			}
		} else
			warningCollector.add(pos, "Unsupported interpolationMode " + interpolationMode);

		handler.endObject(pos, params.polarity);

	}

	private CoordinatePoint calculateCircleCenter(CoordinatePoint start, CoordinatePoint end, CoordinateVector ij,
			QuadrantMode quadrantMode, boolean clockWise) {
		CoordinateVector d = start.vectorTo(end);
		var midPoint = start.plus(d.scale(0.5));
		var midLineD = d.normal();
		if (quadrantMode == QuadrantMode.MULTI) {
			var centerOrig = start.plus(ij);
			return midPoint.projectPointToLine(centerOrig, midLineD);
		} else {
			CoordinatePoint[] candidates = new CoordinatePoint[] { start.plus(ij), start.minus(ij),
					start.plus(ij.x, -ij.y), start.plus(-ij.x, ij.y) };
			CoordinatePoint bestCenter = null;
			Double bestDeviation = null;
			for (CoordinatePoint candidate : candidates) {
				var proj = midPoint.projectPointToLine(candidate, midLineD);
				var span = Math.abs(angle(proj.vectorTo(start).angle(), proj.vectorTo(end).angle(), clockWise));
				if (span > 90)
					continue;
				double deviation = proj.vectorTo(candidate).length();
				if (bestCenter == null || deviation < bestDeviation) {
					bestCenter = proj;
					bestDeviation = deviation;
				}
			}
			return bestCenter;
		}
	}

	double angle(double start, double end, boolean clockWise) {
		if (clockWise) {
			double diff = end - start;
			if (diff > 0)
				diff -= 360;
			return diff;
		} else {
			double diff = end - start;
			if (diff < 0)
				diff += 360;
			return diff;
		}
	}

	@Override
	public void flash(InputPosition pos, CoordinateTransformation transformation, ApertureDefinition aperture,
			Polarity polarity) {

		if (aperture == null) {
			warningCollector.add(pos, "No current aperture for flash operation");
			return;
		}
		handler.beginObject(pos);
		if (aperture.standardTemplate != null) {

			switch (aperture.standardTemplate) {
			case C: {
				var diameter = aperture.parameters.get(0);
				handler.beginPath(pos);
				handler.addArc(pos, transformation, CoordinatePoint.of(-diameter / 2, -diameter / 2), diameter,
						diameter, 0, 360);
				handler.endPath(pos, Exposure.ON);
				if (aperture.parameters.size() >= 2) {
					var hole = aperture.parameters.get(1);
					handler.beginPath(pos);
					handler.addArc(pos, transformation, CoordinatePoint.of(-hole / 2, -hole / 2), hole, hole, 0, 360);
					handler.endPath(pos, Exposure.OFF);
				}
			}
				break;

			case R: {
				var w = aperture.parameters.get(0);
				var h = aperture.parameters.get(1);
				var vx = CoordinateVector.of(w, 0);
				var vy = CoordinateVector.of(0, h);
				var p1 = CoordinatePoint.of(-w / 2, -h / 2);
				var p2 = p1.plus(vx);
				var p3 = p2.plus(vy);
				var p4 = p1.plus(vy);
				handler.beginPath(pos);
				handler.addLine(pos, transformation, p1, p2);
				handler.addLine(pos, transformation, p2, p3);
				handler.addLine(pos, transformation, p3, p4);
				handler.addLine(pos, transformation, p4, p1);
				handler.endPath(pos, Exposure.ON);
				if (aperture.parameters.size() >= 3) {
					var hole = aperture.parameters.get(2);

					handler.beginPath(pos);
					handler.addArc(pos, transformation, CoordinatePoint.of(-hole / 2, -hole / 2), hole, hole, 0, 360);
					handler.endPath(pos, Exposure.OFF);
				}
			}
				break;
			case O: {
				double w = aperture.parameters.get(0);
				double h = aperture.parameters.get(1);
				handler.beginPath(pos);
				if (w < h) {
					// height is bigger
					var p1 = CoordinatePoint.of(-w / 2, h / 2 - w / 2);
					handler.addArc(pos, transformation, p1.minusY(w / 2), w, w, 180, -180);
					var p2 = p1.plusX(w);
					var p3 = CoordinatePoint.of(w / 2, w / 2 - h / 2);
					handler.addLine(pos, transformation, p2, p3);
					var p4 = p3.minusX(w);
					handler.addArc(pos, transformation, p4.minusY(w / 2), w, w, 0, -180);
				} else {
					// width is bigger
					var p1 = CoordinatePoint.of(w / 2 - h / 2, h / 2);
					var p2 = p1.minusY(h);
					handler.addArc(pos, transformation, p2.minusX(h / 2), h, h, 90, -180);
					var p3 = CoordinatePoint.of(h / 2 - w / 2, -h / 2);
					handler.addLine(pos, transformation, p2, p3);
					var p4 = p3.plusY(h);
					handler.addArc(pos, transformation, p3.minusX(h / 2), h, h, -90, -180);
					handler.addLine(pos, transformation, p4, p1);

				}
				handler.endPath(pos, Exposure.ON);
				if (aperture.parameters.size() >= 3) {
					var hole = aperture.parameters.get(2);
					handler.beginPath(pos);
					handler.addArc(pos, transformation, CoordinatePoint.of(-hole / 2, -hole / 2), hole, hole, 0, 360);
					handler.endPath(pos, Exposure.OFF);
				}
			}
				break;
			case P: {
				var diameter = aperture.parameters.get(0);
				var radius = diameter / 2;
				var verticesCount = (int) (double) aperture.parameters.get(1);
				double rotationAngle = 0.;
				if (aperture.parameters.size() >= 3)
					rotationAngle = aperture.parameters.get(2);
				Double holeDiameter = null;
				if (aperture.parameters.size() >= 4)
					holeDiameter = aperture.parameters.get(3);

				double sectionAngle = 360. / verticesCount;
				double startAngle = rotationAngle;
				CoordinatePoint startPoint = CoordinatePoint.ofAngular(radius, startAngle);
				CoordinatePoint lastPoint = startPoint;

				handler.beginPath(pos);
				for (int i = 1; i < verticesCount; i++) {
					double angle = startAngle + i * sectionAngle;
					var p = CoordinatePoint.ofAngular(radius, angle);
					handler.addLine(pos, transformation, lastPoint, p);
					lastPoint = p;
				}
				handler.addLine(pos, transformation, lastPoint, startPoint);
				handler.endPath(pos, Exposure.ON);

				if (holeDiameter != null) {
					handler.beginPath(pos);
					handler.addArc(pos, transformation, CoordinatePoint.of(-holeDiameter / 2, -holeDiameter / 2),
							holeDiameter, holeDiameter, 0, 360);
					handler.endPath(pos, Exposure.OFF);
				}

			}
				break;
			default:
				warningCollector.add(pos, "Unsuported standard apterture " + aperture.standardTemplate);
			}

		} else {
			MacroExpressionEvaluator evaluator = new MacroExpressionEvaluator(warningCollector);
			List<Double> parameters = aperture.parameters;
			for (int i = 0; i < parameters.size(); i++) {
				evaluator.set(i + 1, parameters.get(i));
			}
			System.out.println("Values " + evaluator);
			for (var statement : aperture.template.body.statements) {
				System.out.println("Flashing " + statement);
				statement.accept(new MacroStatementVisitor() {

					@Override
					public void visit(MacroVariableDefinitionStatement macroVariableDefinitionStatement) {
						Double value = evaluator.evaluate(macroVariableDefinitionStatement.exp);
						evaluator.set(macroVariableDefinitionStatement.variableNr, value);
						System.out.println("New Values " + evaluator);
					}

					@Override
					public void visit(MacroPrimitiveComment macroPrimitiveComment) {
						// NOP
					}

					@Override
					public void visit(MacroPrimitiveCircle circle) {
						Double exposureValue = evaluator.evaluate(circle.exposure);
						var diameter = evaluator.evaluate(circle.diameter);
						Double centerX = evaluator.evaluate(circle.centerX);
						Double centerY = evaluator.evaluate(circle.centerY);
						if (exposureValue == null || diameter == null || centerX == null || centerY == null)
							return;
						var exposure = exposureValue == 0 ? Exposure.OFF : Exposure.ON;
						var center = CoordinatePoint.of(centerX, centerY);
						if (circle.rotationAngle != null) {
							var rotation = evaluator.evaluate(circle.rotationAngle);
							if (rotation == null)
								return;
							center = center.rotate(rotation);
						}

						var radius = diameter / 2;

						handler.beginPath(pos);
						handler.addArc(pos, transformation, center.minus(radius, radius), diameter, diameter, 0, 360);
						handler.endPath(pos, exposure);
					}

					@Override
					public void visit(MacroPrimitiveVectorLine line) {
						Double exposureValue = evaluator.evaluate(line.exposure);
						var width = evaluator.evaluate(line.width);
						var startX = evaluator.evaluate(line.startX);
						var startY = evaluator.evaluate(line.startY);
						var endX = evaluator.evaluate(line.endX);
						var endY = evaluator.evaluate(line.endY);
						var rotationValue = evaluator.evaluate(line.rotation);
						if (exposureValue == null || width == null || startX == null || startY == null || endX == null
								|| endY == null || rotationValue == null)
							return;

						double r = rotationValue;
						var start = CoordinatePoint.of(startX, startY).rotate(r);
						var end = CoordinatePoint.of(endX, endY).rotate(r);

						var d = start.vectorTo(end);
						var n = d.normal().normalize().scale(width / 2);
						var exposure = exposureValue == 0 ? Exposure.OFF : Exposure.ON;

						addRectangle(pos, transformation, exposure, start.plus(n), end.plus(n), end.minus(n),
								start.minus(n));
					}

					@Override
					public void visit(MacroPrimitiveCenterLine line) {
						Double exposureValue = evaluator.evaluate(line.exposure);
						var width = evaluator.evaluate(line.width);
						var height = evaluator.evaluate(line.height);
						var centerX = evaluator.evaluate(line.centerX);
						var centerY = evaluator.evaluate(line.centerY);
						var rotationValue = evaluator.evaluate(line.rotation);
						if (exposureValue == null || width == null || height == null || centerX == null
								|| centerY == null || rotationValue == null)
							return;
						var exposure = exposureValue == 0 ? Exposure.OFF : Exposure.ON;
						double r = rotationValue;
						var center = CoordinatePoint.of(centerX, centerY).rotate(r);
						var dw = CoordinateVector.of(width / 2, 0).rotate(r);
						var dh = CoordinateVector.of(0, height / 2).rotate(r);
						addRectangle(pos, transformation, exposure, center.plus(dw).plus(dh), center.plus(dw).minus(dh),
								center.minus(dw).minus(dh), center.minus(dw).plus(dh));
					}

					@Override
					public void visit(MacroPrimitiveOutline line) {
						Double exposureValue = evaluator.evaluate(line.exposure);
						var startX = evaluator.evaluate(line.startX);
						var startY = evaluator.evaluate(line.startY);
						var rotationValue = evaluator.evaluate(line.rotation);
						if (exposureValue == null || startX == null || startY == null || rotationValue == null)
							return;
						List<CoordinatePoint> vertices = new ArrayList<>();
						for (var v : line.vertices) {
							var x = evaluator.evaluate(v.get(0));
							var y = evaluator.evaluate(v.get(1));
							if (x == null || y == null)
								return;
							vertices.add(CoordinatePoint.of(x, y));
						}
						var exposure = exposureValue == 0 ? Exposure.OFF : Exposure.ON;
						double r = rotationValue;
						CoordinatePoint startPoint = CoordinatePoint.of(startX, startY).rotate(r);
						CoordinatePoint lastPoint = startPoint;
						handler.beginPath(pos);
						for (var vertex : vertices) {
							var point = vertex.rotate(r);
							handler.addLine(pos, transformation, lastPoint, point);
							lastPoint = point;

						}
						handler.endPath(pos, exposure);
					}

					@Override
					public void visit(MacroPrimitivePolygon polygon) {
						var exposureValue = evaluator.evaluate(polygon.exposure);
						var numberOfVertices = evaluator.evaluate(polygon.numberOfVertices);
						var centerX = evaluator.evaluate(polygon.centerX);
						var centerY = evaluator.evaluate(polygon.centerY);
						var diameter = evaluator.evaluate(polygon.diameter);
						var rotation = evaluator.evaluate(polygon.rotation);
						if (centerX == null || centerY == null || diameter == null || exposureValue == null
								|| numberOfVertices == null || rotation == null)
							return;
						var r = rotation;

						int verticesCount = (int) (double) numberOfVertices;
						double sectionAngle = 360. / verticesCount;
						var exposure = exposureValue == 0 ? Exposure.OFF : Exposure.ON;
						var radius = diameter / 2;
						var center = CoordinatePoint.of(centerX, centerY);
						CoordinatePoint startPoint = center.plusX(radius).rotate(r);
						CoordinatePoint lastPoint = startPoint;

						handler.beginPath(pos);
						for (int i = 1; i < verticesCount; i++) {
							double angle = i * sectionAngle;
							var p = center.plus(CoordinateVector.of(radius, angle)).rotate(r);
							handler.addLine(pos, transformation, lastPoint, p);
							lastPoint = p;
						}
						handler.addLine(pos, transformation, lastPoint, startPoint);
						handler.endPath(pos, exposure);

					}

					@Override
					public void visit(MacroPrimitiveMoire moire) {
						var centerX = evaluator.evaluate(moire.centerX);
						var centerY = evaluator.evaluate(moire.centerY);
						var diameter = evaluator.evaluate(moire.diameter);
						var thickness = evaluator.evaluate(moire.thickness);
						var gap = evaluator.evaluate(moire.gap);
						var maxRings = evaluator.evaluate(moire.maxRings);
						var crosshairThickness = evaluator.evaluate(moire.crosshairThickness);
						var crosshairLength = evaluator.evaluate(moire.crosshairLength);
						var rotation = evaluator.evaluate(moire.rotation);
						if (centerX == null || centerY == null || diameter == null || thickness == null || gap == null
								|| maxRings == null || crosshairThickness == null || crosshairLength == null
								|| rotation == null)
							return;

						var r = rotation;
						var center = CoordinatePoint.of(centerX, centerY);
						for (int i = 0; i < (int) (double) maxRings; i++) {
							var d = diameter - (2 * i * (thickness + gap));
							if (d <= 0)
								break;
							handler.beginPath(pos);
							handler.addArc(pos, transformation, center.rotate(r).minus(d / 2, d / 2), d, d, 0, 360);
							handler.endPath(pos, Exposure.ON);

							d -= gap * 2;
							if (d <= 0)
								break;
							handler.beginPath(pos);
							handler.addArc(pos, transformation, center.rotate(r).minus(d / 2, d / 2), d, d, 0, 360);
							handler.endPath(pos, Exposure.OFF);
						}

						if (thickness > 0) {
							{
								var p1 = center.plus(-crosshairThickness / 2, crosshairLength / 2).rotate(r);
								var p2 = center.plus(crosshairThickness / 2, crosshairLength / 2).rotate(r);
								var p3 = center.plus(crosshairThickness / 2, -crosshairLength / 2).rotate(r);
								var p4 = center.minus(crosshairThickness / 2, crosshairLength / 2).rotate(r);
								addRectangle(pos, transformation, Exposure.ON, p1, p2, p3, p4);
							}
							{
								var p1 = center.plus(-crosshairLength / 2, crosshairThickness / 2).rotate(r);
								var p2 = center.plus(crosshairLength / 2, crosshairThickness / 2).rotate(r);
								var p3 = center.plus(crosshairLength / 2, -crosshairThickness / 2).rotate(r);
								var p4 = center.minus(crosshairLength / 2, crosshairThickness / 2).rotate(r);
								addRectangle(pos, transformation, Exposure.ON, p1, p2, p3, p4);
							}
						}
					}

					private void addRectangle(InputPosition pos, CoordinateTransformation transformation,
							Exposure exposure, CoordinatePoint p1, CoordinatePoint p2, CoordinatePoint p3,
							CoordinatePoint p4) {
						handler.beginPath(pos);
						handler.addLine(pos, transformation, p1, p2);
						handler.addLine(pos, transformation, p2, p3);
						handler.addLine(pos, transformation, p3, p4);
						handler.addLine(pos, transformation, p4, p1);
						handler.endPath(pos, exposure);
					}

					@Override
					public void visit(MacroPrimitiveThermal thermal) {
						var outerDiameter = evaluator.evaluate(thermal.outerDiameter);
						Double outerRadius = outerDiameter / 2;
						var innerDiameter = evaluator.evaluate(thermal.innerDiameter);
						Double centerX = evaluator.evaluate(thermal.centerX);
						Double centerY = evaluator.evaluate(thermal.centerY);
						Double gap = evaluator.evaluate(thermal.gap);
						if (outerDiameter == null || innerDiameter == null || centerX == null || centerY == null
								|| gap == null)
							return;
						var center = CoordinatePoint.of(centerX, centerY);
						var rotation = evaluator.evaluate(thermal.rotation);
						if (rotation != null) {
							center = center.rotate(rotation);
						}

						handler.beginPath(pos);
						handler.addArc(pos, transformation, center.minus(outerRadius, outerRadius), outerDiameter,
								outerDiameter, 0, 360);
						handler.endPath(pos, Exposure.ON);
						handler.beginPath(pos);
						handler.addArc(pos, transformation, center.minus(innerDiameter / 2, innerDiameter / 2),
								innerDiameter, innerDiameter, 0, 360);
						handler.endPath(pos, Exposure.OFF);

						gap = gap / 2;
						{
							handler.beginPath(pos);
							CoordinatePoint p1 = center.plus(-gap, outerRadius);
							CoordinatePoint p2 = center.plus(gap, outerRadius);
							CoordinatePoint p3 = center.plus(gap, -outerRadius);
							CoordinatePoint p4 = center.minus(gap, outerRadius);
							handler.addLine(pos, transformation, p1, p2);
							handler.addLine(pos, transformation, p2, p3);
							handler.addLine(pos, transformation, p3, p4);
							handler.addLine(pos, transformation, p4, p1);
							handler.endPath(pos, Exposure.OFF);
						}
						{
							handler.beginPath(pos);
							CoordinatePoint p1 = center.plus(outerRadius, gap);
							CoordinatePoint p2 = center.plus(outerRadius, -gap);
							CoordinatePoint p3 = center.minus(outerRadius, gap);
							CoordinatePoint p4 = center.plus(-outerRadius, gap);
							handler.addLine(pos, transformation, p1, p2);
							handler.addLine(pos, transformation, p2, p3);
							handler.addLine(pos, transformation, p3, p4);
							handler.addLine(pos, transformation, p4, p1);
							handler.endPath(pos, Exposure.OFF);
						}

					}

				});
			}
		}

		handler.endObject(pos, polarity);
	}

	@Override
	public void regionBegin(InputPosition pos) {
		handler.beginObject(pos);
	}

	@Override
	public void regionStartContour(InputPosition pos) {
		handler.beginPath(pos);
	}

	@Override
	public void regionInterpolate(InterpolateParameter params) {
		InterpolationMode interpolationMode = params.interpolationMode;
		CoordinatePoint start = params.current;
		CoordinatePoint end = params.target;
		if (interpolationMode == InterpolationMode.LINEAR) {
			handler.addLine(params.pos, params.transformation, start, end);
		} else if (interpolationMode == InterpolationMode.CIRCULAR_CLOCKWISE
				|| interpolationMode == InterpolationMode.CIRCULAR_COUNTER_CLOCKWISE) {
			// circular interpolation
			boolean clockWise = interpolationMode == InterpolationMode.CIRCULAR_CLOCKWISE;
			QuadrantMode quadrantMode = params.quadrantMode;

			if (start.equals(end)) {
				switch (quadrantMode) {
				case MULTI: {
					var radius = params.ij.length();
					handler.addArc(params.pos, params.transformation, start.minus(radius, radius), radius * 2,
							radius * 2, 0, clockWise ? -360 : 360);
				}
					break;
				case SINGLE:
					// NOP
					break;
				default:
					throw new UnsupportedOperationException();
				}
			} else {

				CoordinatePoint center = calculateCircleCenter(start, end, params.ij, quadrantMode, clockWise);
				double startAngle = center.vectorTo(start).angle();
				double endAngle = center.vectorTo(end).angle();

				double radius = center.vectorTo(start).length();
				double diameter = radius * 2;

				handler.addArc(params.pos, params.transformation, center.minus(radius, radius), diameter, diameter,
						startAngle, angle(startAngle, endAngle, clockWise));
			}
		} else
			warningCollector.add(params.pos, "Unsupported interpolationMode " + interpolationMode);
	}

	@Override
	public void regionEndContour(InputPosition pos) {
		handler.endPath(pos, Exposure.ON);
	}

	@Override
	public void regionEnd(InputPosition pos, Polarity polarity) {
		handler.endObject(pos, polarity);
	}

}
