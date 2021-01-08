package com.github.ruediste.gerberLib.read;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinateLength;
import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.parser.InputPosition;

public enum StandardApertureTemplate {
	C {
		@Override
		List<CoordinateLength> validateParameters(List<CoordinateLength> parameters, WarningCollector warningsCollector,
				InputPosition pos) {
			if (parameters.size() < 1) {
				warningsCollector.add(pos, "Circle diameter not given, defaulting to 1 mm");
				return List.of(new CoordinateLength(CoordinateLengthUnit.MM, 1));
			}
			if (parameters.size() > 2) {
				warningsCollector.add(pos, "too many parameters given for circle");
				return parameters.stream().limit(2).collect(toList());
			}
			return parameters;
		}
	},
	R {
		@Override
		List<CoordinateLength> validateParameters(List<CoordinateLength> parameters, WarningCollector warningsCollector,
				InputPosition pos) {
			return parameters;
		}
	},
	O {
		@Override
		List<CoordinateLength> validateParameters(List<CoordinateLength> parameters, WarningCollector warningsCollector,
				InputPosition pos) {
			return parameters;
		}
	},
	P {
		@Override
		List<CoordinateLength> validateParameters(List<CoordinateLength> parameters, WarningCollector warningsCollector,
				InputPosition pos) {
			if (parameters.size() < 1) {
				warningsCollector.add(pos, "Polygon diameter and number of vertices not given, defaulting to 1 mm/6");
				return List.of(new CoordinateLength(CoordinateLengthUnit.MM, 1),
						new CoordinateLength(CoordinateLengthUnit.MM, 6));
			}
			if (parameters.size() < 2) {
				warningsCollector.add(pos, "Polygon number of vertices not given, defaulting to 6");
				parameters.add(new CoordinateLength(CoordinateLengthUnit.MM, 6));
			}
			int verticeCount = (int) parameters.get(1).getOriginalValue();
			if (verticeCount < 3) {
				warningsCollector.add(pos, "Polygon number of vertices too small: " + verticeCount);
				parameters.set(1, new CoordinateLength(CoordinateLengthUnit.MM, 3));
			}
			if (verticeCount > 12) {
				warningsCollector.add(pos, "Polygon number of vertices too large: " + verticeCount);
				parameters.set(1, new CoordinateLength(CoordinateLengthUnit.MM, 12));
			}
			if (parameters.size() > 4) {
				warningsCollector.add(pos, "too many parameters given for polygon");
				return parameters.stream().limit(4).collect(toList());
			}
			return parameters;
		}
	};

	abstract List<CoordinateLength> validateParameters(List<CoordinateLength> parameters,
			WarningCollector warningsCollector, InputPosition pos);
}
