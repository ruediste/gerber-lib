package com.github.ruediste.gerberLib.read;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
				switch (binaryOperation.operation) {
				case DIVIDE:
					return left / right;
				case MINUS:
					return left - right;
				case MULTIPLY:
					return left * right;
				case PLUS:
					return left + right;
				default:
					throw new UnsupportedOperationException();
				}
			}

			@Override
			public Double visit(MacroExpressionUnaryMinus unaryMinus) {
				return -evaluate(unaryMinus.exp);
			}

			@Override
			public Double visit(MacroExpressionVariable variable) {
				Double result = variableValues.get(variable.variableNr);
				if (result == null) {
					return 0.;
				}
				return result;
			}
		});
	}

	@Override
	public String toString() {
		return "{" + variableValues.entrySet().stream().sorted(Comparator.comparing(x -> x.getKey()))
				.map(x -> x.getKey() + "=" + x.getValue()).collect(Collectors.joining(", ")) + "}";
	}
}
