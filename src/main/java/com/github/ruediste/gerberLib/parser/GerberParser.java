package com.github.ruediste.gerberLib.parser;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.gerberLib.read.QuadrantMode;

public class GerberParser extends ParserBase<GerberParsingState> {
	GerberParsingEventHandler handler;
	private GerberMacroBodyParser macroBodyParser;

	public GerberParser(GerberParsingEventHandler handler, String input) {
		super(new ParsingContext<>(input, new GerberParsingState()));
		this.handler = handler;
		macroBodyParser = new GerberMacroBodyParser(ctx);
	}

	public void file() {
		zeroOrMore(this::statement);
		eof();
	}

	private void endOfFile() {
		var pos = ctx.copyPos();
		next("M02*");
		eof();
		handler.endOfFile(pos);
	}

	void statement() {
		eatNewLines();
		choice(this::operation, this::interpolation_state_command, this::setCurrentAperture_Dnn, this::comment_G04,
				this::attribute_command, this::apertureDefinition_AD, this::apertureMacro_AM, this::coordinate_command,
				this::transformation_state_command, this::region_statement, this::SR_statement, this::AB_statement,
				this::endOfFile, this::unknownStatement);

	}

	void unknownStatement() {
		var pos = ctx.copyPos();
		var text = join(sequence(() -> join(zeroOrMore(() -> not("*"))), () -> next("*")));
		optional(() -> choice(() -> nextRaw("%\n"), () -> nextRaw("%\r\n")));
		handler.unknownStatement(pos, text);
	}

	void AB_statement() {
		ctx.throwException("todo: AB");

	}

	void SR_statement() {
		ctx.throwException("todo: SR");

	}

	void region_statement() {
//		next("G36*");
//		oneOrMore(() -> {
//			moveOperation_D02();
//			zeroOrMore(() -> choice(this::interpolateOperation_D01, this::interpolation_state_command));
//		});
//		next("G37*");
		var pos = ctx.copyPos();
		choice(() -> {
			next("G36*");
			handler.beginRegion(pos);
		}, () -> {
			next("G37*");
			handler.endRegion(pos);
		});
	}

	void transformation_state_command() {
		choice(this::LP, this::LM, this::LR, this::LS);

	}

	void LP() {
		InputPosition pos = ctx.copyPos();
		next("%LP");
		ctx.limitBacktracking();
		var polarity = any("CD");
		next("*%");
		handler.loadPolarity(pos, polarity);
	}

	void LM() {
		ctx.throwException("todo: LM");
	}

	void LR() {
		ctx.throwException("todo: LR");
	}

	void LS() {
		ctx.throwException("todo: LS");
	}

	void coordinate_command() {
		choice(this::coordinateFormatSpecification_FS, this::unit_MO);
	}

	void coordinateFormatSpecification_FS() {
		InputPosition pos = ctx.copyPos();
		GerberCoordinateFormatSpecification fmt = new GerberCoordinateFormatSpecification();
		next("%FSLAX");
		ctx.limitBacktracking();
		fmt.xIntegerDigits = Integer.parseInt(any("123456789"));
		fmt.xDecimalDigits = Integer.parseInt(any("56"));
		next("Y");
		fmt.yIntegerDigits = Integer.parseInt(any("123456789"));
		fmt.yDecimalDigits = Integer.parseInt(any("56"));
		next("*%");
		handler.coordinateFormatSpecification(pos, fmt);
	}

	void unit_MO() {
		InputPosition pos = ctx.copyPos();
		next("%MO");
		ctx.limitBacktracking();
		String unit = choice(() -> next("MM"), () -> next("IN"));
		next("*%");
		handler.unit(pos, unit);
	}

	void apertureMacro_AM() {
		InputPosition pos = ctx.copyPos();
		next("%AM");
		ctx.limitBacktracking();
		var name = name();
		next("*");
		var body = macroBodyParser.macroBody();
		next("%");
		handler.apertureMacro(pos, name, body);
	}

	void apertureDefinition_AD() {
		InputPosition pos = ctx.copyPos();
		next("%ADD");
		ctx.limitBacktracking();
		var number = aperture_nr();
		var template = name();
		List<String> parameters = optional(() -> {
			next(",");
			List<String> result = new ArrayList<String>();
			result.add(decimal());
			zeroOrMore(() -> {
				next("X");
				return decimal();
			}).forEach(result::add);
			return result;
		}, List.of());

		next("*%");
		handler.apertureDefinition(pos, number, template, parameters);
	}

	String aperture_nr() {
		// /[1-9][0-9]+/;
		var pos = ctx.copyPos();
		var value = join(oneOrMore(() -> any(isDigit())));
		int valueParsed;
		try {
			valueParsed = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw ctx.throwException("valid integer", pos);
		}
		if (valueParsed < 10)
			ctx.throwException("value>=10", pos);
		return value;
	}

	void attribute_command() {
		choice(this::TO, this::TD, this::TA, this::fileAttributes_TF);
	}

	void TO() {
		ctx.throwException("todo: TO");
	}

	void TD() {
		ctx.throwException("todo: TD");
	}

	void TA() {
		ctx.throwException("todo: TA");
	}

	void fileAttributes_TF() {
		InputPosition pos = ctx.copyPos();
		next("%TF");
		ctx.limitBacktracking();
		var name = choice(this::standard_name, this::user_name);
		var attributes = zeroOrMore(() -> {
			next(",");
			return field();
		});
		optional(() -> next(","));
		next("*%");
		handler.fileAttribute(pos, name, attributes);
	}

	void comment_G04() {
		InputPosition pos = ctx.copyPos();
		next("G04");
		ctx.limitBacktracking();
		String comment = string();
		next("*");
		handler.comment(pos, comment);
	}

	void addEvent(Runnable event) {
		ctx.state.addEvent(event);
	}

	void setCurrentAperture_Dnn() {
		var pos = ctx.copyPos();
		next("D");
		ctx.limitBacktracking();
		var aperture = aperture_nr();
		next("*");
		handler.setCurrentAperture(pos, aperture);
	}

	void interpolation_state_command() {
		choice(this::linearInterpolation_G01, this::G02, this::G03, this::G74, this::G75);

	}

	void linearInterpolation_G01() {
		var pos = ctx.copyPos();
		next("G01*");
		handler.setInterpolationMode(pos, InterpolationMode.LINEAR);
	}

	void G02() {
		var pos = ctx.copyPos();
		next("G02*");
		handler.setInterpolationMode(pos, InterpolationMode.CIRCULAR_CLOCKWISE);
	}

	void G03() {
		var pos = ctx.copyPos();
		next("G03*");
		handler.setInterpolationMode(pos, InterpolationMode.CIRCULAR_COUNTER_CLOCKWISE);
	}

	void G74() {
		var pos = ctx.copyPos();
		next("G74*");
		handler.setQuadrantMode(pos, QuadrantMode.SINGLE);
	}

	void G75() {
		var pos = ctx.copyPos();
		next("G75*");
		handler.setQuadrantMode(pos, QuadrantMode.MULTI);
	}

	void operation() {
		choice(this::interpolateOperation_D01, this::moveOperation_D02, this::flashOperation_D03);
	}

	void interpolateOperation_D01() {
		var pos = ctx.copyPos();
		var x = optional(() -> {
			next("X");
			return coordinate();
		});
		var y = optional(() -> {
			next("Y");
			return coordinate();
		});
		String i = optional(() -> {
			next("I");
			return coordinate();
		});
		String j = optional(() -> {
			next("J");
			return coordinate();
		});
		next("D01*");
		handler.interpolateOperation(pos, x, y, i, j);
	}

	void moveOperation_D02() {
		var pos = ctx.copyPos();
		var x = optional(() -> {
			next("X");
			return coordinate();
		});
		var y = optional(() -> {
			next("Y");
			return coordinate();
		});
		next("D02*");
		handler.moveOperation(pos, x, y);

	}

	void flashOperation_D03() {
		var pos = ctx.copyPos();
		var x = optional(() -> {
			next("X");
			return coordinate();
		});
		var y = optional(() -> {
			next("Y");
			return coordinate();
		});
		next("D03*");
		handler.flashOperation(pos, x, y);
	}

	String field() {
		return join(zeroOrMore(() -> choice(() -> {
			next("\\u");
			return paseUtf(timesString(4, () -> any(hexNumberChars)));
		}, () -> {
			next("\\U");
			return paseUtf(timesString(8, () -> any(hexNumberChars)));
		}, () -> not("*%,"))));
	}

	String decimal() {
		return join(sequence(() -> optional(() -> any("+-")), this::unsigned_decimal));
	}

	String integer() {
		return join(sequence(() -> optional(() -> any("+-")), this::unsigned_integer));
	}

	String coordinate() {
		// /[+-]{0,1}[0-9]+/;
		return join(sequence(() -> optional(() -> any("+-")), () -> join(oneOrMore(() -> any(isDigit())))));
	}

	String name() {
		// /[a-zA-Z_.$][a-zA-Z_.0-9]*/;
		return join(sequence(() -> any(isLetter(), isAny("_.$")),
				() -> join(zeroOrMore(() -> any(isLetter(), isAny("_."), isDigit())))));
	}

	String standard_name() {
		return join(sequence(() -> next("."), () -> any(isLetter(), isAny("_.$")),
				() -> join(times(0, 125, () -> any(isLetter(), isDigit(), isAny("_.$"))))));

	}

	String user_name() {
		return join(sequence(() -> any(isLetter(), isAny("_$")),
				() -> join(times(0, 126, () -> any(isLetter(), isDigit(), isAny("_.$"))))));
	}
}
