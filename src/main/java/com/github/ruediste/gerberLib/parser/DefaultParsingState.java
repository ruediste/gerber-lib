package com.github.ruediste.gerberLib.parser;

public class DefaultParsingState extends ParsingState<DefaultParsingState> {

	@Override
	protected DefaultParsingState copyImpl() {
		return new DefaultParsingState();
	}

}
