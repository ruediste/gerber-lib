package com.github.ruediste.gerberLib.read;

import static java.util.stream.Collectors.toList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.linAlg.CoordinateVector;
import com.github.ruediste.gerberLib.parser.GerberCoordinateFormatSpecification;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroBody;
import com.github.ruediste.gerberLib.parser.GerberParsingEventHandler;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.parser.InterpolationMode;

/**
 * This adapter abstracts the parsing events to graphics commands. It takes care
 * of aperture definitions, block apertures, step and repeat etc. It is the job
 * of the {@link GerberReadGraphicsEventHandler} to handle the graphics
 * primitives.
 *
 */
public class GerberReadGraphicsAdapter implements GerberParsingEventHandler {

	public Map<Integer, ApertureDefinition> aperturesDictionary = new HashMap<>();

	private GerberReadGraphicsEventHandler handler;
	private WarningCollector warningCollector;
	private boolean regionActive;
	private boolean regionContourStarted;

	public GerberGraphicsState state = new GerberGraphicsState();

	public GerberReadGraphicsAdapter(WarningCollector warningCollector, GerberReadGraphicsEventHandler handler) {
		this.warningCollector = warningCollector;
		this.handler = handler;
	}

	@Override
	public void unit(InputPosition pos, String unit) {
		if (state.unit != null)
			throw new RuntimeException("Cannot set the unit multiple times");
		state.unit = CoordinateLengthUnit.valueOf(unit);
	}

	@Override
	public void apertureDefinition(InputPosition pos, int number, String template, List<String> parameters) {
		ApertureDefinition def = new ApertureDefinition();
		def.nr = number;
		Optional<StandardApertureTemplate> standardTemplate = Stream.of(StandardApertureTemplate.values())
				.filter(x -> x.name().equals(template)).findFirst();
		def.parameters = parameters.stream()
				.map(str -> state.unit.convertTo(CoordinateLengthUnit.MM, Double.parseDouble(str))).collect(toList());
		if (standardTemplate.isPresent()) {
			def.standardTemplate = standardTemplate.get();
			def.parameters = def.standardTemplate.validateParameters(def.parameters, warningCollector, null);
		} else {
			def.template = apertureTemplateDictionary.get(template);
			if (def.template == null) {
				warningCollector.add(pos,
						"Unknown aperture template " + template + " for aperture definition D" + number);
				return;
			}
		}
		aperturesDictionary.put(def.nr, def);
	}

	@Override
	public void setCurrentAperture(InputPosition pos, int aperture) {
		state.currentAperture = aperturesDictionary.get(aperture);
		if (state.currentAperture == null)
			warningCollector.add(pos, "aperture " + aperture + " not found");
	}

	@Override
	public void setInterpolationMode(InputPosition pos, InterpolationMode mode) {
		state.interpolationMode = mode;
	}

	@Override
	public void setQuadrantMode(InputPosition pos, QuadrantMode mode) {
		state.quadrantMode = mode;
	}

	@Override
	public void coordinateFormatSpecification(InputPosition pos, GerberCoordinateFormatSpecification format) {
		state.coordinateFormat = format;
	}

	Double parseCoordinateX(String str) {
		return parseCoordinate(str, state.coordinateFormat.xIntegerDigits, state.coordinateFormat.xDecimalDigits);
	}

	Double parseCoordinateY(String str) {
		return parseCoordinate(str, state.coordinateFormat.yIntegerDigits, state.coordinateFormat.yDecimalDigits);
	}

	Double parseCoordinate(String str, int integerDigits, int decimalDigits) {
		if (str == null)
			return null;
		String tmp = str;
		double sign = 1;
		if (tmp.charAt(0) == '-') {
			sign = -1;
			tmp = tmp.substring(1);
		} else if (tmp.charAt(0) == '+') {
			tmp = tmp.substring(1);
		}

		while (tmp.length() < integerDigits + decimalDigits)
			tmp = "0" + tmp;
		double value = sign * Double.parseDouble(tmp.substring(0, integerDigits) + "." + tmp.substring(integerDigits));
		return state.unit.convertTo(CoordinateLengthUnit.MM, value);
	}

	private <T> T orFallback(T value, T fallback) {
		if (value != null)
			return value;
		else
			return fallback;
	}

	@Override
	public void interpolateOperation(InputPosition pos, String x, String y, String iStr, String jStr) {

		if (state.currentX == null) {
			warningCollector.add(pos, "No initial x coordinate given. Defaulting to 0");
			state.currentX = 0.;
		}
		if (state.currentY == null) {
			warningCollector.add(pos, "No initial y coordinate given. Defaulting to 0");
			state.currentY = 0.;
		}

		double targetX = state.currentX;
		double targetY = state.currentY;
		if (x != null)
			targetX = parseCoordinateX(x);
		if (y != null)
			targetY = parseCoordinateY(y);

		Double i = orFallback(parseCoordinateX(iStr), 0.);
		Double j = orFallback(parseCoordinateX(jStr), 0.);

		ApertureDefinition aperture = state.currentAperture;
		CoordinatePoint current = state.current();
		CoordinatePoint target = CoordinatePoint.of(targetX, targetY);
		InterpolationMode interpolationMode = state.interpolationMode;
		QuadrantMode quadrantMode = state.quadrantMode;
		Polarity polarity = state.polarity;
		if (regionActive) {
			if (!regionContourStarted) {
				regionContourStarted = true;
				callHandler(() -> handler.regionStartContour(pos));
			}
			callHandler(() -> {
				InterpolateParameter params = new InterpolateParameter(pos, state.blockTransformations.peek(), current,
						target, CoordinateVector.of(i, j), aperture, interpolationMode, quadrantMode, polarity);
				handler.regionInterpolate(params);
			});
		} else {
			if (aperture == null) {
				warningCollector.add(pos, "No aperture defined, not drawing");
				return;
			}
			// TODO: scaling
			callHandler(() -> {
				InterpolateParameter params = new InterpolateParameter(pos, state.blockTransformations.peek(), current,
						target, CoordinateVector.of(i, j), aperture, interpolationMode, quadrantMode, polarity);
				handler.interpolate(params);
			});
		}
		state.currentX = targetX;
		state.currentY = targetY;
	}

	@Override
	public void moveOperation(InputPosition pos, String x, String y) {
		if (regionContourStarted) {
			callHandler(() -> handler.regionEndContour(pos));
			regionContourStarted = false;
		}
		if (x != null)
			state.currentX = parseCoordinateX(x);
		if (y != null)
			state.currentY = parseCoordinateY(y);
	}

	@Override
	public void unknownStatement(InputPosition pos, String text) {
		warningCollector.add(pos, "Unknown statement: " + text);
	}

	Map<String, ApertureTemplate> apertureTemplateDictionary = new HashMap<>();

	@Override
	public void apertureMacro(InputPosition pos, String name, MacroBody body) {
		ApertureTemplate tmpl = new ApertureTemplate();
		tmpl.name = name;
		tmpl.body = body;
		apertureTemplateDictionary.put(tmpl.name, tmpl);
	}

	@Override
	public void flashOperation(InputPosition pos, String x, String y) {
		if (regionActive) {
			warningCollector.add(pos, "flash not allowed in region");
			return;
		}
		if (x != null)
			state.currentX = parseCoordinateX(x);
		if (y != null)
			state.currentY = parseCoordinateY(y);

		var current = state.current();
		ApertureDefinition aperture = state.currentAperture;
		CoordinateTransformation apertureTransformation = state.apertureTransformation.copy();
		Polarity polarity = state.polarity;
		callHandler(() -> {
			CoordinateTransformation t = state.blockTransformations.peek().copy();
			t.translate(current);
			t.concatenate(apertureTransformation);
			if (aperture.handlerCalls != null) {
				System.out.println(
						"Flash D" + aperture.nr + " current: " + current + " polarity: " + polarity + " trans: " + t);
				state.blockTransformations.push(t);
				aperture.handlerCalls.forEach(Runnable::run);
				state.blockTransformations.pop();
			} else {
				handler.flash(pos, t, aperture, polarity);
			}
		});
	}

	@Override
	public void beginRegion(InputPosition pos) {
		if (regionActive)
			warningCollector.add(pos, "region already started");
		regionActive = true;
		callHandler(() -> handler.regionBegin(pos));
	}

	@Override
	public void endRegion(InputPosition pos) {
		if (!regionActive)
			warningCollector.add(pos, "no active region");
		if (regionContourStarted) {
			regionContourStarted = false;
			callHandler(() -> handler.regionEndContour(pos));
		}
		regionActive = false;
		callHandler(() -> handler.regionEnd(pos, state.polarity));
	}

	@Override
	public void loadPolarity(InputPosition pos, String polarity) {
		if (regionActive) {
			warningCollector.add(pos, "cannot change polarity in region, ignoring");
			return;
		}
		if ("C".equals(polarity))
			state.polarity = Polarity.CLEAR;
		else if ("D".equals(polarity))
			state.polarity = Polarity.DARK;
		else
			warningCollector.add(pos, "Unknown polarity " + polarity);
	}

	CoordinateTransformation blockApertureTransformation;
	int blockApertureDepth;
	Deque<List<Runnable>> handlerCalls = new ArrayDeque<>();

	private void callHandler(Runnable call) {
		if (blockApertureDepth == 0)
			call.run();
		else {
			handlerCalls.peek().add(call);
		}
	}

	@Override
	public void beginBlockAperture(int nr) {
		blockApertureDepth++;
		handlerCalls.push(new ArrayList<>());
		state.currentX = null;
		state.currentY = null;
	}

	@Override
	public void endBlockAperture(int nr) {
		blockApertureDepth--;
		ApertureDefinition def = new ApertureDefinition();
		def.nr = nr;
		def.handlerCalls = handlerCalls.pop();
		aperturesDictionary.put(def.nr, def);
		state.currentX = null;
		state.currentY = null;
	}

	@Override
	public void comment(InputPosition pos, String string) {
		// NOP
	}

	@Override
	public void fileAttribute(InputPosition pos, String name, List<String> attributes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endOfFile(InputPosition pos) {
		// NOP
	}

	@Override
	public void loadMirroring(InputPosition pos, String mirroring) {
		state.mirroring = mirroring;
		state.updateApertureTransformation();
	}

	@Override
	public void loadRotation(InputPosition pos, String rotation) {
		state.rotation = Double.parseDouble(rotation);
		state.updateApertureTransformation();
	}

	@Override
	public void loadScaling(InputPosition pos, String scaling) {
		state.scaling = Double.parseDouble(scaling);
		state.updateApertureTransformation();
	}
}
