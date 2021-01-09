package com.github.ruediste.gerberLib.read;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinateLength;
import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
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
public class GerberReadGraphicsAdapter extends GerberParsingEventHandler {

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
		def.parameters = parameters.stream().map(str -> new CoordinateLength(state.unit, Double.parseDouble(str)))
				.collect(toList());
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

	CoordinateLength parseCoordinateX(String str) {
		return parseCoordinate(str, state.coordinateFormat.xIntegerDigits, state.coordinateFormat.xDecimalDigits);
	}

	CoordinateLength parseCoordinateY(String str) {
		return parseCoordinate(str, state.coordinateFormat.yIntegerDigits, state.coordinateFormat.yDecimalDigits);
	}

	CoordinateLength parseCoordinate(String str, int integerDigits, int decimalDigits) {
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
		return new CoordinateLength(state.unit, value);
	}

	@Override
	public void interpolateOperation(InputPosition pos, String x, String y, String iStr, String jStr) {

		if (state.currentX == null) {
			warningCollector.add(pos, "No initial x coordinate given. Defaulting to 0");
			state.currentX = new CoordinateLength(state.unit, 0);
		}
		if (state.currentY == null) {
			warningCollector.add(pos, "No initial y coordinate given. Defaulting to 0");
			state.currentY = new CoordinateLength(state.unit, 0);
		}

		CoordinateLength targetX = state.currentX;
		CoordinateLength targetY = state.currentY;
		if (x != null)
			targetX = parseCoordinateX(x);
		if (y != null)
			targetY = parseCoordinateY(y);

		CoordinateLength i = parseCoordinateX(iStr);
		if (i == null)
			i = CoordinateLength.ZERO;
		CoordinateLength j = parseCoordinateX(jStr);
		if (j == null)
			j = CoordinateLength.ZERO;

		InterpolateParameter params = new InterpolateParameter(pos, state, CoordinatePoint.of(targetX, targetY),
				CoordinateVector.of(i, j));
		if (regionActive) {
			if (!regionContourStarted) {
				regionContourStarted = true;
				callHandler(() -> handler.regionStartContour(pos, state));
			}
			callHandler(() -> handler.regionInterpolate(params));
		} else {
			if (state.currentAperture == null) {
				warningCollector.add(pos, "No aperture defined, not drawing");
				return;
			}
			callHandler(() -> handler.interpolate(params));
		}
		state.currentX = targetX;
		state.currentY = targetY;
	}

	@Override
	public void moveOperation(InputPosition pos, String x, String y) {
		if (regionContourStarted) {
			callHandler(() -> handler.regionEndContour(pos, state));
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

		if (state.currentAperture.handlerCalls != null) {
			state.currentAperture.handlerCalls.forEach(Runnable::run);
		} else {
			callHandler(() -> handler.flash(pos, state));
		}
	}

	@Override
	public void beginRegion(InputPosition pos) {
		if (regionActive)
			warningCollector.add(pos, "region already started");
		regionActive = true;
		callHandler(() -> handler.regionBegin(pos, state));
	}

	@Override
	public void endRegion(InputPosition pos) {
		if (!regionActive)
			warningCollector.add(pos, "no active region");
		if (regionContourStarted) {
			regionContourStarted = false;
			callHandler(() -> handler.regionEndContour(pos, state));
		}
		regionActive = false;
		callHandler(() -> handler.regionEnd(pos, state));
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

	int blockApertureDepth;
	List<Runnable> handlerCalls = new ArrayList<>();

	private void callHandler(Runnable call) {
		if (blockApertureDepth == 0)
			call.run();
		else {
			handlerCalls.add(call);
		}
	}

	@Override
	public void beginBlockAperture(int nr) {
		blockApertureDepth++;
	}

	@Override
	public void endBlockAperture(int nr) {
		blockApertureDepth--;
		ApertureDefinition def = new ApertureDefinition();
		def.nr = nr;
		def.handlerCalls = handlerCalls;
		handlerCalls = new ArrayList<>();
		aperturesDictionary.put(def.nr, def);
	}
}
