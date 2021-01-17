package com.github.ruediste.gerberLib.parser;

import java.util.List;

import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroBody;
import com.github.ruediste.gerberLib.read.QuadrantMode;

public interface GerberParsingEventHandler {

	public void comment(InputPosition pos, String string);

	public void coordinateFormatSpecification(InputPosition pos, GerberCoordinateFormatSpecification format);

	public void unit(InputPosition pos, String unit);

	public void loadPolarity(InputPosition pos, String polarity);

	public void apertureDefinition(InputPosition pos, int number, String template, List<String> parameters);

	public void setCurrentAperture(InputPosition pos, int aperture);

	public void interpolateOperation(InputPosition pos, String x, String y, String i, String j);

	public void moveOperation(InputPosition pos, String x, String y);

	public void flashOperation(InputPosition pos, String x, String y);

	public void setInterpolationMode(InputPosition pos, InterpolationMode linear);

	public void endOfFile(InputPosition pos);

	public void unknownStatement(InputPosition pos, String text);

	public void apertureMacro(InputPosition pos, String name, MacroBody body);

	public void beginRegion(InputPosition pos);

	public void endRegion(InputPosition pos);

	public void setQuadrantMode(InputPosition pos, QuadrantMode mode);

	public void beginBlockAperture(int nr);

	public void endBlockAperture(int nr);

	public void loadMirroring(InputPosition pos, String mirroring);

	public void loadRotation(InputPosition pos, String rotation);

	public void loadScaling(InputPosition pos, String scaling);

	public void beginStepAndRepeat(InputPosition pos);

	public void endStepAndRepeat(InputPosition pos, String xRepeats, String yRepeats, String xDistance,
			String yDistance);

	public void fileAttribute(InputPosition pos, String name, List<String> attributes);

	public void objectAttribute(InputPosition pos, String name, List<String> attributes);

	public void deleteAttribute(InputPosition pos, String name);

	public void apertureAttribute(InputPosition pos, String name, List<String> attributes);

}
