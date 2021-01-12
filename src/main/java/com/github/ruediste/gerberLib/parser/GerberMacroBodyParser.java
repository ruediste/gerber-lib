package com.github.ruediste.gerberLib.parser;

import java.util.List;

public class GerberMacroBodyParser extends ParserBase<GerberParsingState> {

	public GerberMacroBodyParser(ParsingContext<GerberParsingState> ctx) {
		super(ctx);
	}

	public static class MacroBody {

		public List<MacroStatement> statements;
	}

	public MacroBody macroBody() {
		MacroBody body = new MacroBody();
		body.statements = oneOrMore(this::macroStatement);
		return body;
	}

	public interface MacroStatementVisitor {

		void visit(MacroPrimitiveThermal macroPrimitiveThermal);

		void visit(MacroPrimitiveMoire macroPrimitiveMoire);

		void visit(MacroPrimitivePolygon macroPrimitivePolygon);

		void visit(MacroPrimitiveOutline macroPrimitiveOutline);

		void visit(MacroPrimitiveCenterLine macroPrimitiveCenterLine);

		void visit(MacroPrimitiveVectorLine macroPrimitiveVectorLine);

		void visit(MacroPrimitiveCircle macroPrimitiveCircle);

		void visit(MacroPrimitiveComment macroPrimitiveComment);

		void visit(MacroVariableDefinitionStatement macroVariableDefinitionStatement);

	}

	public abstract static class MacroStatement {
		public abstract void accept(MacroStatementVisitor visitor);

		@Override
		public abstract String toString();
	}

	MacroStatement macroStatement() {
		return choice(this::primitive, this::variable_definition);
	}

	public abstract static class MacroPrimitiveStatement extends MacroStatement {
	}

	public static class MacroVariableDefinitionStatement extends MacroStatement {

		public int variableNr;
		public MacroExpression exp;

		public MacroVariableDefinitionStatement(int variableNr, MacroExpression exp) {
			this.variableNr = variableNr;
			this.exp = exp;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "$" + variableNr + "=" + exp;
		}
	}

	MacroPrimitiveStatement primitive() {
		MacroPrimitiveStatement primitive = choice(() -> {
			next("0");
			var comment = string();
			return new MacroPrimitiveComment(comment);
		}, () -> {
			next("1");
			return new MacroPrimitiveCircle(par(), par(), par(), par(), optional(this::par));
		}, () -> {
			next("20");
			return new MacroPrimitiveVectorLine(par(), par(), par(), par(), par(), par(), par());
		}, () -> {
			next("21");
			return new MacroPrimitiveCenterLine(par(), par(), par(), par(), par(), par());
		}, () -> {
			next("4");
			return new MacroPrimitiveOutline(par(), par(), par(), par(), oneOrMore(() -> times(2, this::par)), par());
		}, () -> {
			next("5");
			return new MacroPrimitivePolygon(par(), par(), par(), par(), par(), par());
		}, () -> {
			next("6");
			return new MacroPrimitiveMoire(par(), par(), par(), par(), par(), par(), par(), par(), par());
		}, () -> {
			next("7");
			return new MacroPrimitiveThermal(par(), par(), par(), par(), par(), par());
		});
		next("*");
		return primitive;
	}

	public static class MacroPrimitiveComment extends MacroPrimitiveStatement {

		public String comment;

		public MacroPrimitiveComment(String comment) {
			this.comment = comment;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "// " + comment;
		}
	}

	public static class MacroPrimitiveCircle extends MacroPrimitiveStatement {

		public MacroExpression exposure;
		public MacroExpression diameter;
		public MacroExpression centerX;
		public MacroExpression centerY;
		public MacroExpression rotationAngle;

		public MacroPrimitiveCircle(MacroExpression exposure, MacroExpression diameter, MacroExpression centerX,
				MacroExpression centerY, MacroExpression rotationAngle) {
			this.exposure = exposure;
			this.diameter = diameter;
			this.centerX = centerX;
			this.centerY = centerY;
			this.rotationAngle = rotationAngle;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "circle exp:" + exposure + " diameter: " + diameter + " centerX:" + centerX + " centerY:" + centerY
					+ " rotation:" + rotationAngle;
		}
	}

	public static class MacroPrimitiveVectorLine extends MacroPrimitiveStatement {

		public MacroExpression exposure;
		public MacroExpression width;
		public MacroExpression startX;
		public MacroExpression startY;
		public MacroExpression endX;
		public MacroExpression endY;
		public MacroExpression rotation;

		public MacroPrimitiveVectorLine(MacroExpression exposure, MacroExpression width, MacroExpression startX,
				MacroExpression startY, MacroExpression endX, MacroExpression endY, MacroExpression rotation) {
			this.exposure = exposure;
			this.width = width;
			this.startX = startX;
			this.startY = startY;
			this.endX = endX;
			this.endY = endY;
			this.rotation = rotation;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "vectorLine exp:" + exposure + " width: " + width + " startX:" + startX + " startY:" + startY
					+ " endX:" + endX + " endY:" + endY + " rotation:" + rotation;
		}
	}

	public static class MacroPrimitiveCenterLine extends MacroPrimitiveStatement {

		public MacroExpression exposure;
		public MacroExpression width;
		public MacroExpression height;
		public MacroExpression centerX;
		public MacroExpression centerY;
		public MacroExpression rotation;

		public MacroPrimitiveCenterLine(MacroExpression exposure, MacroExpression width, MacroExpression height,
				MacroExpression centerX, MacroExpression centerY, MacroExpression rotation) {
			this.exposure = exposure;
			this.width = width;
			this.height = height;
			this.centerX = centerX;
			this.centerY = centerY;
			this.rotation = rotation;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "centerLine exp:" + exposure + " width: " + width + " height:" + height + " centerX:" + centerX
					+ " centerY:" + centerY + " rotation:" + rotation;
		}
	}

	public static class MacroPrimitiveOutline extends MacroPrimitiveStatement {

		public MacroExpression exposure;
		public MacroExpression numVertices;
		public MacroExpression startX;
		public MacroExpression startY;
		public List<List<MacroExpression>> vertices;
		public MacroExpression rotation;

		public MacroPrimitiveOutline(MacroExpression exposure, MacroExpression numVertices, MacroExpression startX,
				MacroExpression startY, List<List<MacroExpression>> vertices, MacroExpression rotation) {
			this.exposure = exposure;
			this.numVertices = numVertices;
			this.startX = startX;
			this.startY = startY;
			this.vertices = vertices;
			this.rotation = rotation;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "outline exp:" + exposure + " numVertices: " + numVertices + " startX:" + startX + " startY:"
					+ startY + " vertices:" + vertices + " rotation:" + rotation;
		}
	}

	public static class MacroPrimitivePolygon extends MacroPrimitiveStatement {

		public MacroExpression exposure;
		public MacroExpression numberOfVertices;
		public MacroExpression centerX;
		public MacroExpression centerY;
		public MacroExpression diameter;
		public MacroExpression rotation;

		public MacroPrimitivePolygon(MacroExpression exposure, MacroExpression numberOfVertices,
				MacroExpression centerX, MacroExpression centerY, MacroExpression diameter, MacroExpression rotation) {
			this.exposure = exposure;
			this.numberOfVertices = numberOfVertices;
			this.centerX = centerX;
			this.centerY = centerY;
			this.diameter = diameter;
			this.rotation = rotation;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "polygon exp:" + exposure + " numberOfVertices:" + numberOfVertices + " centerX:" + centerX
					+ " centerY:" + centerY + " diameter: " + diameter + " rotation:" + rotation;

		}
	}

	public static class MacroPrimitiveMoire extends MacroPrimitiveStatement {

		public MacroExpression centerX;
		public MacroExpression centerY;
		public MacroExpression diameter;
		public MacroExpression thickness;
		public MacroExpression gap;
		public MacroExpression maxRings;
		public MacroExpression crosshairThickness;
		public MacroExpression crosshairLength;
		public MacroExpression rotation;

		public MacroPrimitiveMoire(MacroExpression centerX, MacroExpression centerY, MacroExpression diameter,
				MacroExpression thickness, MacroExpression gap, MacroExpression maxRings,
				MacroExpression crosshairThickness, MacroExpression crosshairLength, MacroExpression rotation) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.diameter = diameter;
			this.thickness = thickness;
			this.gap = gap;
			this.maxRings = maxRings;
			this.crosshairThickness = crosshairThickness;
			this.crosshairLength = crosshairLength;
			this.rotation = rotation;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "moire centerX:" + centerX + " centerY:" + centerY + " diameter: " + diameter + " thickness:"
					+ thickness + " gap:" + gap + " maxRings:" + maxRings + " crosshairThickness:" + crosshairThickness
					+ " crosshairLength:" + crosshairLength + " rotation:" + rotation;

		}
	}

	public static class MacroPrimitiveThermal extends MacroPrimitiveStatement {

		public MacroExpression centerX;
		public MacroExpression centerY;
		public MacroExpression outerDiameter;
		public MacroExpression innerDiameter;
		public MacroExpression gap;
		public MacroExpression rotation;

		public MacroPrimitiveThermal(MacroExpression centerX, MacroExpression centerY, MacroExpression outerDiameter,
				MacroExpression innerDiameter, MacroExpression gap, MacroExpression rotation) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.outerDiameter = outerDiameter;
			this.innerDiameter = innerDiameter;
			this.gap = gap;
			this.rotation = rotation;
		}

		@Override
		public void accept(MacroStatementVisitor visitor) {
			visitor.visit(this);
		}

		@Override
		public String toString() {
			return "thermal centerX:" + centerX + " centerY:" + centerY + " outerDiameter: " + outerDiameter
					+ " innerDiameter:" + innerDiameter + " gap:" + gap + " rotation:" + rotation;
		}
	}

	public interface MacroExpressionVisitor<T> {

		T visit(MacroExpressionValue macroExpressionValue);

		T visit(MacroExpressionBinaryOperation macroExpressionBinaryOperation);

		T visit(MacroExpressionUnaryMinus macroExpressionUnaryMinus);

		T visit(MacroExpressionVariable macroExpressionVariable);
	}

	public abstract static class MacroExpression {
		public abstract <T> T accept(MacroExpressionVisitor<T> visitor);

		@Override
		public abstract String toString();
	}

	public static class MacroExpressionValue extends MacroExpression {

		public String value;

		public MacroExpressionValue(String value) {
			this.value = value;
		}

		@Override
		public <T> T accept(MacroExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return value;
		}
	}

	public static class MacroExpressionVariable extends MacroExpression {

		public int variableNr;
		public InputPosition pos;

		public MacroExpressionVariable(InputPosition pos, int variableNr) {
			this.pos = pos;
			this.variableNr = variableNr;
		}

		@Override
		public <T> T accept(MacroExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "$" + variableNr;
		}
	}

	public enum MacroExpressionOp {
		PLUS, MINUS, MULTIPLY, DIVIDE
	}

	public static class MacroExpressionUnaryMinus extends MacroExpression {
		public MacroExpressionUnaryMinus(MacroExpression exp) {
			this.exp = exp;
		}

		public MacroExpression exp;

		@Override
		public <T> T accept(MacroExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "-(" + exp + ")";
		}
	}

	public static class MacroExpressionBinaryOperation extends MacroExpression {
		public MacroExpression left;
		public MacroExpressionOp operation;
		public MacroExpression right;

		public MacroExpressionBinaryOperation(MacroExpression left, MacroExpressionOp operation,
				MacroExpression right) {
			this.left = left;
			this.operation = operation;
			this.right = right;
		}

		@Override
		public <T> T accept(MacroExpressionVisitor<T> visitor) {
			return visitor.visit(this);
		}

		@Override
		public String toString() {
			return "(" + left + ")" + operation + "(" + right + ")";
		}
	}

	MacroExpression expression() {
		return choice(() -> addExpr(), () -> {
			var sign = optional(() -> any("+-"));
			var exp = addExpr();
			if ("-".equals(sign)) {
				return new MacroExpressionUnaryMinus(exp);
			} else
				return exp;
		});
	}

	MacroExpression addExpr() {
		var left = mulExpr();
		while (true) {
			var leftFinal = left;
			var opt = optional(() -> {
				var op = any("+-");
				var right = mulExpr();
				return new MacroExpressionBinaryOperation(leftFinal,
						"+".contentEquals(op) ? MacroExpressionOp.PLUS : MacroExpressionOp.MINUS, right);
			});
			if (opt == null)
				return left;
			left = opt;
		}
	}

	MacroExpression mulExpr() {
		var left = factor();
		while (true) {
			var leftFinal = left;
			var opt = optional(() -> {
				var op = any("x/");
				var right = factor();
				return new MacroExpressionBinaryOperation(leftFinal,
						"x".contentEquals(op) ? MacroExpressionOp.MULTIPLY : MacroExpressionOp.DIVIDE, right);
			});
			if (opt == null)
				return left;
			left = opt;
		}
	}

	MacroExpression factor() {
		return choice(() -> {
			next("(");
			var exp = expression();
			next(")");
			return exp;
		}, () -> new MacroExpressionValue(unsigned_decimal()), () -> {
			return new MacroExpressionVariable(ctx.copyPos(), macro_variable());
		});
	}

	int macro_variable() {
		next("$");
		return Integer.parseInt(pos_integer());
	}

	MacroExpression par() {
		next(",");
		return expression();
	}

	MacroVariableDefinitionStatement variable_definition() {
		var nr = macro_variable();
		next("=");
		var exp = expression();
		next("*");
		return new MacroVariableDefinitionStatement(nr, exp);
	}
}
