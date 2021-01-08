package com.github.ruediste.gerberLib.read;

import java.util.HashMap;
import java.util.Map;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.linAlg.CoordinateLength;
import com.github.ruediste.gerberLib.linAlg.CoordinateLengthUnit;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpression;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionBinaryOperation;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionUnaryMinus;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionValue;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionVariable;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionVisitor;

public class MacroExpressionEvaluator {
	Map<Integer, CoordinateLength> variableValues = new HashMap<>();
	private CoordinateLengthUnit unit;
	private WarningCollector warningCollector;

	public MacroExpressionEvaluator(CoordinateLengthUnit unit, WarningCollector warningCollector) {
		this.unit = unit;
		this.warningCollector = warningCollector;

	}

	public void set(int variableNr, CoordinateLength value) {
		variableValues.put(variableNr, value);
	}

	public CoordinateLength evaluate(MacroExpression expr) {
		if (expr == null)
			return null;
		return expr.accept(new MacroExpressionVisitor<CoordinateLength>() {

			@Override
			public CoordinateLength visit(MacroExpressionValue value) {
				return CoordinateLength.of(unit, Double.parseDouble(value.value));
			}

			@Override
			public CoordinateLength visit(MacroExpressionBinaryOperation binaryOperation) {
				var left = evaluate(binaryOperation.left);
				var right = evaluate(binaryOperation.right);
				if (left == null || right == null)
					return null;
				switch (binaryOperation.operation) {
				case DIVIDE:
					return CoordinateLength.of(unit, left.getValue(unit) / right.getValue(unit));
				case MINUS:
					return left.minus(right);
				case MULTIPY:
					return CoordinateLength.of(unit, left.getValue(unit) * right.getValue(unit));
				case PLUS:
					return left.plus(right);
				default:
					throw new UnsupportedOperationException();
				}
			}

			@Override
			public CoordinateLength visit(MacroExpressionUnaryMinus unaryMinus) {
				CoordinateLength value = evaluate(unaryMinus.exp);
				if (value == null)
					return null;
				return value.scale(-1);
			}

			@Override
			public CoordinateLength visit(MacroExpressionVariable variable) {
				CoordinateLength result = variableValues.get(variable.variableNr);
				if (result == null) {
					warningCollector.add(variable.pos, "Variable $" + variable.variableNr + " not defined");
				}
				return result;
			}
		});
	}
}
