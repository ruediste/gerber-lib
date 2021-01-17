package com.github.ruediste.gerberLib.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.ruediste.gerberLib.read.QuadrantMode;

public class GerberParser extends ParserBase<GerberParsingState> {
	GerberParsingEventHandler handler;
	GerberMacroBodyParser macroBodyParser;

	public GerberParser(GerberParsingEventHandler handler, String input) {
		super(new ParsingContext<>(input, new GerberParsingState()));
		this.handler = handler;
		macroBodyParser = new GerberMacroBodyParser(ctx);
	}

	public void file() {
		ctx.throwNiceParseException(() -> {
			zeroOrMore(this::statement);
			endOfFile();
		});
	}

	void statement() {
		eatNewLines();
		if (!choiceNT(this::single_statement, this::compound_statement))
			ctx.throwException();

	}

	boolean single_statement() {
		return choiceNT(this::operation, this::interpolation_state_command, this::setCurrentAperture_Dnn,
				this::comment_G04, this::attribute_command, this::apertureDefinition_AD, this::apertureMacro_AM,
				this::coordinate_command, this::transformation_state_command);
	}

	boolean compound_statement() {
		return choiceNT(this::region_statement, this::stepAndRepeat_SR_statement, this::apertureBlock_AB_statement,
				this::unknownStatement);
	}

	private void endOfFile() {
		var pos = ctx.copyPos();
		next("M02*");
		eof();
		handler.endOfFile(pos);
	}

	boolean unknownStatement() {
		var pos = ctx.copyPos();
		// negative look ahead
		not("M02*", () -> next("M02"));
		var text = join(sequence(() -> join(zeroOrMore(() -> not("*"))), () -> next("*")));
		optional(() -> choice(() -> nextRaw("%\n"), () -> nextRaw("%\r\n")));
		handler.unknownStatement(pos, text);
		return true;
	}

	void in_block_statement() {
		eatNewLines();
		if (!choiceNT(this::single_statement, this::region_statement, this::apertureBlock_AB_statement))
			throw ctx.throwException();
	}

	boolean apertureBlock_AB_statement() {
		if (!nextNT("%AB"))
			return false;
		ctx.limitBacktracking();
		next("D");
		var nr = aperture_nr();
		next("*%");
		ctx.limitBacktracking();
		handler.beginBlockAperture(nr);
		zeroOrMore(this::in_block_statement);
		ctx.limitBacktracking();
		next("%AB*%");
		handler.endBlockAperture(nr);
		return true;
	}

	boolean stepAndRepeat_SR_statement() {
		var pos = ctx.copyPos();
		if (!nextNT("%SR"))
			return false;
		ctx.limitBacktracking();
		next("X");
		var xRepeats = integer();
		next("Y");
		var yRepeats = integer();
		next("I");
		var xDistance = decimal();
		next("J");
		var yDistance = decimal();
		next("*%");
		ctx.limitBacktracking();
		handler.beginStepAndRepeat(pos);
		zeroOrMore(this::in_block_statement);
		ctx.limitBacktracking();
		pos = ctx.copyPos();
		next("%SR*%");
		handler.endStepAndRepeat(pos, xRepeats, yRepeats, xDistance, yDistance);
		return true;
	}

	boolean region_statement() {
		var pos = ctx.copyPos();
		return choiceNT(() -> {
			if (!nextNT("G36*"))
				return false;
			handler.beginRegion(pos);
			return true;
		}, () -> {
			if (!nextNT("G37*"))
				return false;
			handler.endRegion(pos);
			return true;
		});
	}

	boolean transformation_state_command() {
		return choiceNT(this::LP, this::LM, this::LR, this::LS);
	}

	boolean LP() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%LP"))
			return false;
		ctx.limitBacktracking();
		var polarity = any("CD");
		next("*%");
		handler.loadPolarity(pos, polarity);
		return true;
	}

	boolean LM() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%LM"))
			return false;
		ctx.limitBacktracking();
		var mirroring = choice(() -> next("N"), () -> next("XY"), () -> next("X"), () -> next("Y"));
		next("*%");
		handler.loadMirroring(pos, mirroring);
		return true;
	}

	boolean LR() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%LR"))
			return false;
		ctx.limitBacktracking();
		var rotation = decimal();
		next("*%");
		handler.loadRotation(pos, rotation);
		return true;
	}

	boolean LS() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%LS"))
			return false;
		ctx.limitBacktracking();
		var scaling = decimal();
		next("*%");
		handler.loadScaling(pos, scaling);
		return true;
	}

	boolean coordinate_command() {
		return choiceNT(this::coordinateFormatSpecification_FS, this::unit_MO);
	}

	boolean coordinateFormatSpecification_FS() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%FSLAX"))
			return false;

		GerberCoordinateFormatSpecification fmt = new GerberCoordinateFormatSpecification();
		ctx.limitBacktracking();
		fmt.xIntegerDigits = Integer.parseInt(any("123456789"));
		fmt.xDecimalDigits = Integer.parseInt(any("123456789"));
		next("Y");
		fmt.yIntegerDigits = Integer.parseInt(any("123456789"));
		fmt.yDecimalDigits = Integer.parseInt(any("123456789"));
		next("*%");
		handler.coordinateFormatSpecification(pos, fmt);
		return true;
	}

	boolean unit_MO() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%MO"))
			return false;
		ctx.limitBacktracking();
		String unit = choice(() -> next("MM"), () -> next("IN"));
		next("*%");
		handler.unit(pos, unit);
		return true;
	}

	boolean apertureMacro_AM() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%AM"))
			return false;
		ctx.limitBacktracking();
		var name = name();
		next("*");
		var body = macroBodyParser.macroBody();
		next("%");
		handler.apertureMacro(pos, name, body);
		return true;
	}

	boolean apertureDefinition_AD() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%ADD"))
			return false;
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
		return true;
	}

	int aperture_nr() {
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
		return valueParsed;
	}

	boolean attribute_command() {
		return choiceNT(this::objectAttributes_TO, this::deleteAttribute_TD, this::apertureAttributes_TA,
				this::fileAttributes_TF);
	}

	boolean objectAttributes_TO() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%TO")) {
			return false;
		}
		ctx.limitBacktracking();
		var name = attributeName(); // choice(this::standard_name, this::user_name);
		var attributes = attributeFields();
		next("*%");
		handler.objectAttribute(pos, name, attributes);
		return true;
	}

	String attributeName() {
		// optimezed for performance
		// return join(oneOrMore(() -> any(isAny(".$_"), isLetter(), isDigit())));
		StringBuilder sb = new StringBuilder();
		while (true) {
			int cp = peekCp();
			if (cp == '.' || cp == '$' || cp == '_' || ('a' <= cp && cp <= 'z') || ('A' <= cp && cp <= 'Z')
					|| ('0' <= cp && cp <= '9')) {
				sb.appendCodePoint(nextCp());
				cp = peekCp();
				continue;
			}
			break;
		}
		if (sb.length() == 0)
			throw ctx.throwException("[.$_a-zA-Z0-9]");
		return sb.toString();
	}

	boolean deleteAttribute_TD() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%TD")) {
			return false;
		}
		ctx.limitBacktracking();
		var name = optional(this::attributeName); // choice(this::standard_name, this::user_name);
		next("*%");
		handler.deleteAttribute(pos, name);
		return true;
	}

	boolean apertureAttributes_TA() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%TA")) {
			return false;
		}
		ctx.limitBacktracking();
		var name = attributeName(); // choice(this::standard_name, this::user_name);
		var attributes = attributeFields();
		next("*%");
		handler.apertureAttribute(pos, name, attributes);
		return true;
	}

	boolean fileAttributes_TF() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("%TF")) {
			return false;
		}
		ctx.limitBacktracking();
		var name = attributeName(); // choice(this::standard_name, this::user_name);
		var attributes = attributeFields();
		next("*%");
		handler.fileAttribute(pos, name, attributes);
		return true;
	}

	private List<String> attributeFields() {
		List<String> attributes = new ArrayList<>();
		while (true) {
			int cp = peekCp();
			if (cp != ',')
				return attributes;
			nextCp();
			attributes.add(field());
		}

//		var attributes = zeroOrMore(() -> {
//			next(",");
//			return field();
//		});
//		return attributes;
	}

	String field() {
		StringBuilder sb = new StringBuilder();
		while (true) {
			int cp = peekCp();
			if (cp == '\\') {
				// enter escaping
				nextCp();
				cp = peekCp();
				int l = 4;
				if (cp == 'u') {
					l = 4;
					nextCp();
				} else if (cp == 'U') {
					l = 8;
					nextCp();
				}
				StringBuilder codePoint = new StringBuilder();
				for (int i = 0; i < l; i++) {
					cp = nextCp();
					if ((cp >= '0' && cp <= '9') || (cp >= 'a' && cp <= 'f') || (cp >= 'A' && cp <= 'F')) {
						codePoint.appendCodePoint(cp);
					} else
						throw ctx.throwException("[0-9a-zA-Z]");
				}
				sb.appendCodePoint(Integer.parseInt(codePoint.toString(), 16));
				continue;
			}
			if (cp == '*' || cp == '%' || cp == ',')
				break;
			sb.appendCodePoint(nextCp());
		}
		return sb.toString();
//		return join(zeroOrMore(() -> choice(() -> {
//			next("\\u");
//			return paseUtf(timesString(4, () -> any(hexNumberChars)));
//		}, () -> {
//			next("\\U");
//			return paseUtf(timesString(8, () -> any(hexNumberChars)));
//		}, () -> not("*%,"))));
	}

	boolean comment_G04() {
		InputPosition pos = ctx.copyPos();
		if (!nextNT("G04"))
			return false;
		ctx.limitBacktracking();
		String comment = string();
		next("*");
		handler.comment(pos, comment);
		return true;
	}

	void addEvent(Runnable event) {
		ctx.state.addEvent(event);
	}

	boolean setCurrentAperture_Dnn() {
		var pos = ctx.copyPos();
		if (!nextNT("D"))
			return false;
		ctx.limitBacktracking();
		var aperture = aperture_nr();
		next("*");
		handler.setCurrentAperture(pos, aperture);
		return true;
	}

	boolean interpolation_state_command() {
		return choiceNT(this::linearInterpolation_G01, this::G02, this::G03, this::G74, this::G75);

	}

	boolean linearInterpolation_G01() {
		var pos = ctx.copyPos();
		if (!nextNT("G01"))
			return false;
		// optional *
		if (peekCp() == '*')
			nextCp();
		handler.setInterpolationMode(pos, InterpolationMode.LINEAR);
		return true;
	}

	boolean G02() {
		var pos = ctx.copyPos();
		if (!nextNT("G02"))
			return false;
		// optional *
		if (peekCp() == '*')
			nextCp();
		handler.setInterpolationMode(pos, InterpolationMode.CIRCULAR_CLOCKWISE);
		return true;
	}

	boolean G03() {
		var pos = ctx.copyPos();
		if (!nextNT("G03"))
			return false;

		// optional *
		if (peekCp() == '*')
			nextCp();
		handler.setInterpolationMode(pos, InterpolationMode.CIRCULAR_COUNTER_CLOCKWISE);
		return true;
	}

	boolean G74() {
		var pos = ctx.copyPos();
		if (!nextNT("G74*"))
			return false;
		handler.setQuadrantMode(pos, QuadrantMode.SINGLE);
		return true;
	}

	boolean G75() {
		var pos = ctx.copyPos();
		if (!nextNT("G75*"))
			return false;
		handler.setQuadrantMode(pos, QuadrantMode.MULTI);
		return true;
	}

	boolean operation() {
		// a choice of D01, D02 and D03, optimized for performance
		var pos = ctx.copyPos();
		int cp = nextCp();

		String x;
		if (cp == 'X') {
			x = coordinate();
			cp = nextCp();
		} else
			x = null;

		String y;
		if (cp == 'Y') {
			y = coordinate();
			cp = nextCp();
		} else
			y = null;

		String i;
		if (cp == 'I') {
			i = coordinate();
			cp = nextCp();
		} else
			i = null;

		String j;
		if (cp == 'J') {
			j = coordinate();
			cp = nextCp();
		} else
			j = null;

		Set<String> expectedSet = Set.of("D01*", "D02*", "D03*");
		if (cp != 'D') {
			ctx.expected(expectedSet);
			return false;
		}
		if (nextCp() != '0') {
			ctx.expected(expectedSet);
			return false;
		}

		cp = nextCp();
		if (nextCp() != '*') {
			ctx.expected(expectedSet);
			return false;
		}
		switch (cp) {
		case '1':
			handler.interpolateOperation(pos, x, y, i, j);
			break;
		case '2':
			handler.moveOperation(pos, x, y);
			break;
		case '3':
			handler.flashOperation(pos, x, y);
			break;
		default:
			return false;
		}
		return true;

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

	String decimal() {
		return join(sequence(() -> optional(() -> any("+-")), this::unsigned_decimal));
	}

	String integer() {
		return join(sequence(() -> optional(() -> any("+-")), this::unsigned_integer));
	}

	String coordinate() {
		// /[+-]{0,1}[0-9]+/;
		// return join(sequence(() -> optional(() -> any("+-")), () -> join(oneOrMore(()
		// -> any(isDigit())))));
		StringBuilder sb = new StringBuilder();
		int cp = peekCp();
		if (cp == '+' || cp == '-') {
			sb.appendCodePoint(cp);
			nextCp();
			cp = peekCp();
		}

		if (cp < '0' || cp > '9')
			ctx.throwException("[0-9]");
		while (true) {
			if (cp < '0' || cp > '9')
				break;
			sb.appendCodePoint(cp);
			nextCp();
			cp = peekCp();
		}
		return sb.toString();
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
