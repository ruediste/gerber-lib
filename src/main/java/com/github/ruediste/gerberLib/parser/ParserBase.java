package com.github.ruediste.gerberLib.parser;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ParserBase<S extends ParsingState<S>> {

	protected static String hexNumberChars = "0123456789abcdefABCDEF";
	protected final ParsingContext<S> ctx;

	public ParserBase(ParsingContext<S> ctx) {
		this.ctx = ctx;
	}

	@SafeVarargs
	final protected void choice(Runnable... choices) {
		if (choices.length == 0)
			throw ctx.throwException("any choice");

		S startState = ctx.state.copy();
		for (Runnable choice : choices) {
			try {
				choice.run();
				return;
			} catch (ParseException e) {
				if (startState.pos.inputIndex < ctx.backtrackingLimit)
					throw e;
				ctx.state = startState.copy();
			}
		}
		throw ctx.throwException("any choice");
	}

	/**
	 * Variant of {@link #choice(Runnable...)} supporting non throwable behavior.
	 * Choices can still throw parse exceptions, in which false is returned as well
	 */
	@SafeVarargs
	final protected boolean choiceNT(BooleanSupplier... choices) {
		if (choices.length == 0) {
			ctx.expected("any choice");
			return false;
		}

		S startState = ctx.state.copy();
		for (BooleanSupplier choice : choices) {
			try {
				if (choice.getAsBoolean())
					return true;
			} catch (ParseException e) {
				// swallow
			}
			if (startState.pos.inputIndex < ctx.backtrackingLimit)
				return false;
			ctx.state = startState.copy();
		}
		return false;
	}

	@SafeVarargs
	final protected <T> T choice(Supplier<T>... choices) {
		if (choices.length == 0)
			throw ctx.throwException("any choice");

		ctx.state.enterChoice();
		S startState = ctx.state.copy();
		for (Supplier<T> choice : choices) {
			try {
				T result = choice.get();
				ctx.state.leaveChoice();
				return result;
			} catch (ParseException e) {
				if (startState.pos.inputIndex < ctx.backtrackingLimit)
					throw e;
				ctx.state = startState.copy();
			}
		}
		throw ctx.throwException("any choice");
	}

	protected void optional(Runnable branch) {
		S startState = ctx.state.copy();
		try {
			branch.run();
		} catch (ParseException e) {
			// swallow exception here, might still be registered as latest exception in the
			// context
			ctx.state = startState;
		}
	}

	protected <T> T optional(Supplier<T> branch) {
		return optional(branch, null);
	}

	protected <T> T optional(Supplier<T> branch, T fallback) {
		S startState = ctx.state.copy();
		try {
			return branch.get();
		} catch (ParseException e) {
			// swallow exception here, might still be registered as latest exception in the
			// context
			ctx.state = startState;
			return fallback;
		}
	}

	protected void zeroOrMore(Runnable element) {
		while (true) {
			S startState = ctx.state.copy();
			try {
				element.run();
			} catch (ParseException e) {
				// swallow exception here, might still be registered as latest exception in the
				// context
				ctx.state = startState;
				break;
			}
		}
	}

	protected <T> List<T> zeroOrMore(Supplier<T> element) {
		List<T> result = new ArrayList<T>();
		while (true) {
			S startState = ctx.state.copy();
			try {
				result.add(element.get());
			} catch (ParseException e) {
				// swallow exception here, might still be registered as latest exception in the
				// context
				ctx.state = startState;
				break;
			}
		}
		return result;
	}

	protected void oneOrMore(Runnable element) {
		element.run();
		zeroOrMore(element);
	}

	protected <T> List<T> oneOrMore(Supplier<T> element) {
		List<T> result = new ArrayList<T>();
		result.add(element.get());
		result.addAll(zeroOrMore(element));
		return result;
	}

	<T> List<T> times(int nr, Supplier<T> element) {
		List<T> result = new ArrayList<T>();
		for (int i = 0; i < nr; i++) {
			result.add(element.get());
		}
		return result;
	}

	<T> List<T> times(int min, int max, Supplier<T> element) {
		List<T> result = new ArrayList<T>();
		int i;
		for (i = 0; i < min; i++) {
			result.add(element.get());
		}
		for (; i < max; i++) {
			var start = ctx.state.copy();
			try {
				result.add(element.get());
			} catch (ParseException e) {
				ctx.state = start;
				break;
			}
		}
		return result;
	}

	protected void eof() {
		while (true) {
			if (ctx.isEof())
				break;
			int cp = ctx.nextCp();
			if (cp == '\r' || cp == '\n')
				continue;
			ctx.throwException("EOF");
		}
	}

	@SafeVarargs
	protected final <T> List<T> sequence(Supplier<T>... elements) {
		List<T> result = new ArrayList<T>();
		for (var element : elements) {
			result.add(element.get());
		}
		return result;
	}

	/**
	 * Read a string and make sure it matches the expected String, ignoring \r and
	 * \n characters. This matches behavior matches the gerber specification.
	 * 
	 * @return
	 */
	protected String next(String expected) {
		if (nextNT(expected))
			return expected;
		throw ctx.throwException();
	}

	protected boolean nextNT(String expected) {
		eatNewLines();
		S startState = ctx.state.copy();
		for (int idx = 0; idx < expected.length(); idx = expected.offsetByCodePoints(idx, 1)) {
			int cp = expected.codePointAt(idx);

			while (true) {
				if (ctx.isEof()) {
					ctx.state = startState;
					ctx.expected(expected);
					return false;
				}
				int actual = ctx.nextCp();
				if (actual == '\r' || actual == '\n')
					continue;
				if (actual != cp) {
					ctx.state = startState;
					ctx.expected(expected);
					return false;
				}
				break;
			}
		}
		return true;
	}

	protected String nextRaw(String expected) {
		eatNewLines();
		S startState = ctx.state.copy();
		expected.codePoints().forEach(cp -> {
			while (true) {
				if (ctx.isEof()) {
					ctx.state = startState;
					ctx.throwException(expected);
				}
				int actual = ctx.nextCp();
				if (actual != cp) {
					ctx.state = startState;
					ctx.throwException(expected);
				}
				break;
			}
		});
		return expected;
	}

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

	protected String not(String forbiddenChars) {
		int cp = peekCp();
		if (forbiddenChars.codePoints().anyMatch(x -> x == cp)) {
			ctx.throwException("[^" + forbiddenChars + "]");
		}
		nextCp();
		return new String(new int[] { cp }, 0, 1);
	}

	protected String any(String allowedChars) {
		return any(isAny(allowedChars));

	}

	protected String any(NamedIntPredicate... matchers) {
		int cp = peekCp();
		if (!Arrays.asList(matchers).stream().anyMatch(x -> x.test(cp))) {
			ctx.throwException(Stream.of(matchers).map(x -> x.name()).collect(toSet()));
		}
		nextCp();
		return new String(new int[] { cp }, 0, 1);
	}

	protected String join(List<String> strings) {
		return strings.stream().filter(x -> x != null).collect(joining());
	}

	protected NamedIntPredicate isLetter() {
		return NamedIntPredicate.of("[A-Za-z]", cp -> ('a' <= cp && cp <= 'z') || ('A' <= cp && cp <= 'Z'));
	}

	protected NamedIntPredicate isDigit() {
		return NamedIntPredicate.of("[0-9]", cp -> '0' <= cp && cp <= '9');
	}

	protected NamedIntPredicate isDigitNonZero() {
		return NamedIntPredicate.of("[1-9]", cp -> '1' <= cp && cp <= '9');
	}

	protected NamedIntPredicate isAny(String chars) {
		return NamedIntPredicate.of("[" + chars + "]", cp -> chars.codePoints().anyMatch(x -> cp == x));
	}

	protected void eatNewLines() {
		while (!ctx.isEof()) {
			var cp = ctx.peekCp();
			if (cp == '\n' || cp == '\r') {
				ctx.nextCp();
				continue;
			}
			break;
		}
	}

	protected String timesString(int nr, Supplier<String> element) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nr; i++) {
			sb.append(element.get());
		}
		return sb.toString();
	}

	protected String paseUtf(String escaped) {
		return new String(new int[] { Integer.parseInt(escaped, 16) }, 0, 1);
	}

	protected String string() {
		return join(zeroOrMore(() -> choice(() -> {
			next("\\u");
			return paseUtf(timesString(4, () -> any(hexNumberChars)));
		}, () -> {
			next("\\U");
			return paseUtf(timesString(8, () -> any(hexNumberChars)));
		}, () -> not("*%"))));
	}

	protected String unsigned_decimal() {
		return choice(
				// /[1-9][0-9]*\.[0-9]*/
				() -> join(sequence(() -> join(zeroOrMore(() -> any(isDigit()))), () -> next("."),
						() -> join(zeroOrMore(() -> any(isDigit()))))),

				this::unsigned_integer);
	}

	protected String unsigned_integer() {
		return choice(() -> next("0"), this::pos_integer);
	}

	String pos_integer() {
		// /[1-9][0-9]*/;
		return join(sequence(() -> any(isDigitNonZero()), () -> join(zeroOrMore(() -> any(isDigit())))));
	}

	protected void not(String description, Runnable notExpected) {
		S start = ctx.state.copy();
		boolean matched = false;
		try {
			notExpected.run();
			matched = true;
		} catch (ParseException e) {
			// swallow, reset to start
			ctx.state = start;
		}
		if (matched)
			ctx.throwException(description);
	}
}
