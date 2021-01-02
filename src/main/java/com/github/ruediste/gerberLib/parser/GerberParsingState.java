package com.github.ruediste.gerberLib.parser;

public class GerberParsingState extends ParsingState<GerberParsingState> {

	@Override
	protected GerberParsingState copyImpl() {
		return new GerberParsingState();
	}

}
