package com.github.ruediste.gerberLib.parser;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GerberParser extends ParserBase<GerberParsingState> {
	GerberParsingEventHandler handler;

	public GerberParser(GerberParsingEventHandler handler, String input) {
		super(new ParsingContext<>(input, new GerberParsingState()));
		this.handler = handler;
	}

	public void file() {
		zeroOrMore(this::statement);
		var pos = ctx.copyPos();
		next("M02*");
		eof();
		handler.endOfFile(pos);
	}

	void statement() {
		choice(this::operation, this::interpolation_state_command, this::setCurrentAperture_Dnn, this::comment_G04,
				this::attribute_command, this::apertureDefinition_AD, this::AM, this::coordinate_command,
				this::transformation_state_command, this::region_statement, this::SR_statement, this::AB_statement);

	}

	void AB_statement() {
		ctx.throwException("todo");

	}

	void SR_statement() {
		ctx.throwException("todo");

	}

	void region_statement() {
		ctx.throwException("todo");

	}

	void transformation_state_command() {
		choice(this::LP, this::LM, this::LR, this::LS);

	}

	void LP() {
		InputPosition pos = ctx.copyPos();
		next("%LP");
		var polarity = any("CD");
		next("*%");
		handler.loadPolarity(pos, polarity);
	}

	void LM() {
		ctx.throwException("todo");
	}

	void LR() {
		ctx.throwException("todo");
	}

	void LS() {
		ctx.throwException("todo");
	}

	void coordinate_command() {
		choice(this::coordinateFormatSpecification_FS, this::unit_MO);
	}

	void coordinateFormatSpecification_FS() {
		InputPosition pos = ctx.copyPos();
		GerberCoordinateFormatSpecification fmt = new GerberCoordinateFormatSpecification();
		next("%FSLAX");
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
		String unit = choice(() -> next("MM"), () -> next("IN"));
		next("*%");
		handler.unit(pos, unit);
	}

	void AM() {
		ctx.throwException("todo");

	}

	void apertureDefinition_AD() {
		InputPosition pos = ctx.copyPos();
		next("%AD");
		var number = aperture_ident();
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

	String aperture_ident() {
		// /D[1-9][0-9]+/;
		var pos = ctx.copyPos();
		next("D");
		var value = join(oneOrMore(() -> any(isDigit())));
		int valueParsed;
		try {
			valueParsed = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw ctx.throwException("valid integer", pos);
		}
		if (valueParsed < 10)
			ctx.throwException("value>=10", pos);
		return "D" + value;
	}

	void attribute_command() {
		choice(this::TO, this::TD, this::TA, this::fileAttributes_TF);
	}

	void TO() {
		ctx.throwException("todo");
	}

	void TD() {
		ctx.throwException("todo");
	}

	void TA() {
		ctx.throwException("todo");
	}

	void fileAttributes_TF() {
		InputPosition pos = ctx.copyPos();
		next("%TF");
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
		String comment = string();
		next("*");
		handler.comment(pos, comment);
	}

	void addEvent(Runnable event) {
		ctx.state.addEvent(event);
	}

	void setCurrentAperture_Dnn() {
		var pos = ctx.copyPos();
		var aperture = aperture_ident();
		next("*");
		handler.setCurrentAperture(pos, aperture);
	}

	void interpolation_state_command() {
		choice(this::linearInterpolation_G01, this::G02, this::G03, this::G74, this::G75);

	}

	void linearInterpolation_G01() {
		var pos = ctx.copyPos();
		next("G01*");
		handler.linearIntrepolation(pos);
	}

	void G02() {
		ctx.throwException("todo");
	}

	void G03() {
		ctx.throwException("todo");
	}

	void G74() {
		ctx.throwException("todo");
	}

	void G75() {
		ctx.throwException("todo");
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
		var ij = optional(() -> {
			next("I");
			var i = coordinate();
			next("J");
			var j = coordinate();
			return List.of(i, j);
		});
		next("D01*");
		handler.interpolateOperation(pos, x, y, ij);
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
		next("D02*");
		handler.flashOperation(pos, x, y);
	}

	/**
	 * Read a string and make sure it matches the expected String, ignoring \r and
	 * \n characters. This matches behavior matches the gerber specification.
	 * 
	 * @return
	 */
	String next(String expected) {
		GerberParsingState startState = ctx.state.copy();
		expected.codePoints().forEach(cp -> {
			while (true) {
				if (ctx.isEof()) {
					ctx.state = startState;
					ctx.throwException(expected);
				}
				int actual = ctx.nextCp();
				if (actual == '\r' || actual == '\n')
					continue;
				if (actual != cp) {
					ctx.state = startState;
					ctx.throwException(expected);
				}
				break;
			}
		});
		return expected;
	}

	private static String hexNumberChars = "0123456789abcdefABCDEF";

	int nextCp() {
		int cp;
		while (true) {
			cp = ctx.nextCp();
			if (cp == '\n' || cp == '\r')
				continue;
			break;
		}
		;
		return cp;
	}

	int peekCp() {
		for (int i = 0;; i++) {
			int cp = ctx.peekCp(i);
			if (cp != '\n' && cp != '\r')
				return cp;
		}
	}

	String not(String forbiddenChars) {
		int cp = peekCp();
		if (forbiddenChars.codePoints().anyMatch(x -> x == cp)) {
			ctx.throwException("[^" + forbiddenChars + "]");
		}
		nextCp();
		return new String(new int[] { cp }, 0, 1);
	}

	String any(String allowedChars) {
		return any("[" + allowedChars + "]", cp -> allowedChars.codePoints().anyMatch(x -> x == cp));
	}

	public interface NamedIntPredicate extends IntPredicate {
		String name();

		static NamedIntPredicate of(String name, IntPredicate delegate) {
			return new NamedIntPredicate() {

				@Override
				public boolean test(int value) {
					return delegate.test(value);
				}

				@Override
				public String name() {
					return name;
				}
			};
		}
	}

	String any(NamedIntPredicate... matchers) {
		int cp = peekCp();
		if (!Arrays.asList(matchers).stream().anyMatch(x -> x.test(cp))) {
			ctx.throwException(Stream.of(matchers).map(x -> x.name()).collect(toSet()));
		}
		nextCp();
		return new String(new int[] { cp }, 0, 1);
	}

	String any(String description, IntPredicate... matchers) {
		int cp = peekCp();
		if (!Arrays.asList(matchers).stream().anyMatch(x -> x.test(cp))) {
			ctx.throwException(description);
		}
		nextCp();
		return new String(new int[] { cp }, 0, 1);
	}

	String join(List<String> strings) {
		return strings.stream().filter(x -> x != null).collect(joining());
	}

	String timesString(int nr, Supplier<String> element) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nr; i++) {
			sb.append(element.get());
		}
		return sb.toString();
	}

	String paseUtf(String escaped) {
		return new String(new int[] { Integer.parseInt(escaped, 16) }, 0, 1);
	}

	String string() {
		return join(zeroOrMore(() -> choice(() -> {
			next("\\u");
			return paseUtf(timesString(4, () -> any(hexNumberChars)));
		}, () -> {
			next("\\U");
			return paseUtf(timesString(8, () -> any(hexNumberChars)));
		}, () -> not("*%"))));
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

	NamedIntPredicate isLetter() {
		return NamedIntPredicate.of("[A-Za-z]", cp -> ('a' <= cp && cp <= 'z') || ('A' <= cp && cp <= 'Z'));
	}

	NamedIntPredicate isDigit() {
		return NamedIntPredicate.of("[0-9]", cp -> '0' <= cp && cp <= '9');
	}

	NamedIntPredicate isDigitNonZero() {
		return NamedIntPredicate.of("[1-9]", cp -> '1' <= cp && cp <= '9');
	}

	NamedIntPredicate isAny(String chars) {
		return NamedIntPredicate.of("[" + chars + "]", cp -> chars.codePoints().anyMatch(x -> cp == x));
	}

	String decimal() {
		return join(sequence(() -> optional(() -> any("+-")), this::unsigned_decimal));
	}

	String unsigned_decimal() {
		return choice(
				// /[1-9][0-9]*\.[0-9]*/
				() -> join(sequence(() -> any(isDigitNonZero()), () -> join(zeroOrMore(() -> any(isDigit()))),
						() -> next("."), () -> join(zeroOrMore(() -> any(isDigit()))))),
				// /0?\.[0-9]*/
				() -> join(sequence(() -> optional(() -> next("0")), () -> next("."),
						() -> join(zeroOrMore(() -> any(isDigit()))))),
				this::unsigned_integer);
	}

	String unsigned_integer() {
		return choice(() -> next("0"), this::pos_integer);
	}

	String integer() {
		return join(sequence(() -> optional(() -> any("+-")), this::unsigned_integer));
	}

	String pos_integer() {
		// /[1-9][0-9]*/;
		return join(sequence(() -> any(isDigitNonZero()), () -> join(zeroOrMore(() -> any(isDigit())))));
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
