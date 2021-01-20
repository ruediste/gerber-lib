package com.github.ruediste.gerberLib.jts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFilter;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.ItemBoundable;
import org.locationtech.jts.index.strtree.ItemDistance;
import org.locationtech.jts.index.strtree.STRtree;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.parser.InputPosition;

public class MoveGenerator {

	private MoveHandler handler;
	private WarningCollector warningCollector;
	private List<TreeItem> treeItems = new ArrayList<>();

	public MoveGenerator(WarningCollector warningCollector, MoveHandler handler) {
		this.warningCollector = warningCollector;
		this.handler = handler;

	}

	public void add(Geometry geometry) {
		geometry.apply(new GeometryFilter() {

			@Override
			public void filter(Geometry g) {
				if (g instanceof LineString) {
					add((LineString) g);
				} else if (g instanceof Polygon) {
					Polygon polygon = (Polygon) g;
					add(polygon.getExteriorRing());
					for (int i = 0; i < polygon.getNumInteriorRing(); i++)
						add(polygon.getInteriorRingN(i));
				} else if (g instanceof GeometryCollection) {
					// NOP
				} else if (g instanceof org.locationtech.jts.geom.Point) {
					// NOP
				} else
					warningCollector.add(new InputPosition(), "unsupported geometry " + g);
			}

		});
	}

	public void add(LineString lineString) {
		if (lineString.isEmpty())
			return;
		LineStringWrapper wrapper = new LineStringWrapper();
		wrapper.isClosed = lineString.isClosed();
		for (int n = 0; n < lineString.getNumPoints() - (wrapper.isClosed ? 1 : 0); n++) {
			Coordinate coord = lineString.getCoordinateN(n);
			treeItems.add(new TreeItem(coord, n, wrapper));
		}
		wrapper.max = wrapper.items.size() - 1;
	}

	static class LineStringWrapper {
		public List<TreeItem> items = new ArrayList<>();
		public int min;
		public int max;
		public boolean isClosed;
	}

	static class TreeItem {
		Coordinate p;
		int n;
		LineStringWrapper wrapper;

		public TreeItem(Coordinate p, int n, LineStringWrapper wrapper) {
			this.p = p;
			this.n = n;
			this.wrapper = wrapper;
			wrapper.items.add(this);
		}

		@Override
		public String toString() {
			return "" + n;
		}

	}

	public void generateMoves(Coordinate startingPoint) {
		Collections.shuffle(treeItems);
		var tree = new STRtree();
		treeItems.forEach(item -> tree.insert(new Envelope(item.p), item));

		handler.moveTo(startingPoint);

		Coordinate p = startingPoint;
		while (!tree.isEmpty()) {
			Envelope env = new Envelope(p);
			TreeItem closest = (TreeItem) tree.nearestNeighbour(env, null, new ItemDistance() {

				@Override
				public double distance(ItemBoundable item1, ItemBoundable item2) {
					if (item1.getItem() == item2.getItem())
						return Double.MAX_VALUE;
					return ((Envelope) item1.getBounds()).distance((Envelope) item2.getBounds());
				}
			});
			if (closest == null)
				break;

			handler.moveTo(closest.p);

			p = processLineString(closest, (item) -> handler.lineTo(item.p),
					item -> tree.remove(new Envelope(item.p), item));
		}
	}

	Coordinate processLineString(TreeItem closest, Consumer<TreeItem> consumer, Consumer<TreeItem> remove) {
		var wrapper = closest.wrapper;
		Coordinate latestPoint = closest.p;
		if (wrapper.isClosed) {
			// just iterate forward over whole ring
			int i = closest.n;
			while (true) {
				i++;
				if (i == wrapper.items.size()) {
					i = 0;
				}
				var item = wrapper.items.get(i);
				consumer.accept(item);
				latestPoint = item.p;
				remove.accept(item);
				if (i == closest.n)
					break; // we went all the way around
			}
		} else {
			if (wrapper.max != closest.n) {
				// to to end of the line string
				int i = closest.n;
				if (closest.n == wrapper.min)
					remove.accept(closest);

				while (true) {
					i++;
					if (i > wrapper.max)
						break;
					var item = wrapper.items.get(i);
					consumer.accept(item);
					latestPoint = item.p;
					if (i != closest.n)
						remove.accept(item);
				}
				wrapper.max = closest.n; // visit the first point again when drawing the rest
			}

			else {
				// to the beginning of the line string
				int i = closest.n;
				if (closest.n == wrapper.max)
					remove.accept(closest);
				while (true) {
					i--;
					if (i < wrapper.min)
						break;
					var item = wrapper.items.get(i);
					consumer.accept(item);
					latestPoint = item.p;
					if (i != closest.n)
						remove.accept(item);
				}
				wrapper.min = closest.n; // visit the first point again when drawing the rest
			}
		}
		return latestPoint;
	}
}
