package com.github.ruediste.gerberLib.read;

import java.util.ArrayList;
import java.util.List;

public class ApertureDefinition {
	public StandardApertureTemplate standardTemplate;
	public ApertureTemplate template;
	public List<Double> parameters = new ArrayList<>();
	public int nr;
	List<Runnable> handlerCalls;
}
