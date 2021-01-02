package com.github.ruediste.gerberLib.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ParserBase<S extends ParsingState<S>> {

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
				ctx.state = startState.copy();
			}
		}
		throw ctx.latestException;
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
				ctx.state = startState.copy();
			}
		}
		throw ctx.latestException;
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
}
