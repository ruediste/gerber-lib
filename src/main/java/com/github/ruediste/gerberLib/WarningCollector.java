package com.github.ruediste.gerberLib;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.gerberLib.parser.InputPosition;

public class WarningCollector {

	public static class Waring {
		public InputPosition pos;
		public String message;

		public Waring(InputPosition pos, String message) {
			super();
			this.pos = pos;
			this.message = message;
		}

	}

	public final List<Waring> warnings = new ArrayList<>();

	public void add(InputPosition pos, String message) {
		warnings.add(new Waring(pos, message));
	}
}
