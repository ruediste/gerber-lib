package com.github.ruediste.gerberLib.jts;

import org.locationtech.jts.geom.Coordinate;

public interface MoveHandler {

	void moveTo(Coordinate coordinate);

	void lineTo(Coordinate coordinate);
}
