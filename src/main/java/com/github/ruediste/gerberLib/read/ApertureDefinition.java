package com.github.ruediste.gerberLib.read;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.gerberLib.linAlg.CoordinateLength;

public class ApertureDefinition {
	public StandardApertureTemplate standardTemplate;
	public ApertureTemplate template;
	public List<CoordinateLength> parameters = new ArrayList<CoordinateLength>();
	public int nr;
}
