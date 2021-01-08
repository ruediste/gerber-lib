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
		}

		);
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
	}

	public interface MacroExpressionVisitor<T> {

		T visit(MacroExpressionValue macroExpressionValue);

		T visit(MacroExpressionBinaryOperation macroExpressionBinaryOperation);

		T visit(MacroExpressionUnaryMinus macroExpressionUnaryMinus);

		T visit(MacroExpressionVariable macroExpressionVariable);
	}

	public abstract static class MacroExpression {
		public abstract <T> T accept(MacroExpressionVisitor<T> visitor);
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
	}

	public enum MacroExpressionOp {
		PLUS, MINUS, MULTIPY, DIVIDE
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
	}

	MacroExpression expression() {
		var sign = optional(() -> any("+-"));
		var exp = unsigned_expression();
		if ("-".equals(sign)) {
			return new MacroExpressionUnaryMinus(exp);
		} else
			return exp;
	}

	MacroExpression unsigned_expression() {
		var left = term();
		return optional(() -> {
			var op = any("+-");
			var right = unsigned_expression();
			return new MacroExpressionBinaryOperation(left,
					"+".contentEquals(op) ? MacroExpressionOp.PLUS : MacroExpressionOp.MINUS, right);
		}, left);

	}

	MacroExpression term() {
		var left = factor();
		return optional(() -> {
			var op = any("x/");
			var right = term();
			return new MacroExpressionBinaryOperation(left,
					"x".contentEquals(op) ? MacroExpressionOp.MULTIPY : MacroExpressionOp.DIVIDE, right);
		}, left);
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
		return new MacroVariableDefinitionStatement(nr, exp);
	}
}
