package com.github.ruediste.gerberLib.parser;

import java.util.List;

public class GerberParsingEventHandler {

	public void comment(InputPosition pos, String string) {
	}

	public void coordinateFormatSpecification(InputPosition pos, GerberCoordinateFormatSpecification format) {
	}

	public void unit(InputPosition pos, String unit) {
	}

	public void fileAttribute(InputPosition pos, String name, List<String> attributes) {

	}

	public void loadPolarity(InputPosition pos, String polarity) {

	}

	public void apertureDefinition(InputPosition pos, String number, String template, List<String> parameters) {

	}

	public void setCurrentAperture(InputPosition pos, String aperture) {
	}

	public void interpolateOperation(InputPosition pos, String x, String y, List<String> ij) {

	}

	public void moveOperation(InputPosition pos, String x, String y) {

	}

	public void flashOperation(InputPosition pos, String x, String y) {

	}

	public void linearIntrepolation(InputPosition pos) {

	}

	public void endOfFile(InputPosition pos) {

	}

}
