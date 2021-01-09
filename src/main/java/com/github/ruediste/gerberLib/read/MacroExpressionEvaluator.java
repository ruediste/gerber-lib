package com.github.ruediste.gerberLib.read;

import java.util.HashMap;
import java.util.Map;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpression;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionBinaryOperation;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionUnaryMinus;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionValue;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionVariable;
import com.github.ruediste.gerberLib.parser.GerberMacroBodyParser.MacroExpressionVisitor;

public class MacroExpressionEvaluator {
	Map<Integer, Double> variableValues = new HashMap<>();
	private WarningCollector warningCollector;

	public MacroExpressionEvaluator(WarningCollector warningCollector) {
		this.warningCollector = warningCollector;

	}

	public void set(int variableNr, double value) {
		variableValues.put(variableNr, value);
	}

	public Double evaluate(MacroExpression expr) {
		if (expr == null)
			return null;
		return expr.accept(new MacroExpressionVisitor<Double>() {

			@Override
			public Double visit(MacroExpressionValue value) {
				return Double.parseDouble(value.value);
			}

			@Override
			public Double visit(MacroExpressionBinaryOperation binaryOperation) {
				var left = evaluate(binaryOperation.left);
				var right = evaluate(binaryOperation.right);
				if (left == null || right == null)
					return null;
				switch (binaryOperation.operation) {
				case DIVIDE:
					return left / right;
				case MINUS:
					return left - right;
				case MULTIPY:
					return left * right;
				case PLUS:
					return left + right;
				default:
					throw new UnsupportedOperationException();
				}
			}

			@Override
			public Double visit(MacroExpressionUnaryMinus unaryMinus) {
				Double value = evaluate(unaryMinus.exp);
				if (value == null)
					return null;
				return -value;
			}

			@Override
			public Double visit(MacroExpressionVariable variable) {
				Double result = variableValues.get(variable.variableNr);
				if (result == null) {
					warningCollector.add(variable.pos, "Variable $" + variable.variableNr + " not defined");
				}
				return result;
			}
		});
	}
}
