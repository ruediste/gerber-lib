package com.github.ruediste.gerberLib.parser;

import java.util.ArrayList;
import java.util.List;

public abstract class ParsingState<T extends ParsingState<T>> {
	public InputPosition pos = new InputPosition();
	int choiceDepth;

	public final T copy() {
		T result = copyImpl();
		result.pos = pos.copy();
		result.choiceDepth = choiceDepth;
		return result;
	}

	protected abstract T copyImpl();

	public int nextCp(ParsingContext<?> ctx) {
		if (pos.inputIndex >= ctx.input.length())
			ctx.throwException("any character");

		int ch = ctx.input.codePointAt(pos.inputIndex);
		pos.inputIndex = ctx.input.offsetByCodePoints(pos.inputIndex, 1);
		if (ch == '\n') {
			pos.lineNr++;
			pos.linePos = 1;
			pos.lineStartIndex = pos.inputIndex;
		} else
			pos.linePos++;
		return ch;
	}

	void enterChoice() {
		choiceDepth++;
	}

	void leaveChoice() {
		choiceDepth--;
		if (choiceDepth == 0) {
			queuedEvents.forEach(x -> x.run());
			queuedEvents.clear();
		}
	}

	private List<Runnable> queuedEvents = new ArrayList<>();

	public void addEvent(Runnable event) {
		if (choiceDepth == 0)
			event.run();
		else
			queuedEvents.add(event);
	}
}
