package com.github.ruediste.gerberLib.jts;

import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.awt.ShapeReader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.rasterizer.Java2dRendererBase;
import com.github.ruediste.gerberLib.read.Polarity;

public class JtsAdapter extends Java2dRendererBase {

	private GeometryFactory gf;
	private Quadtree imageTree;
	private List<Geometry> currentObject;
	private GeometryPrecisionReducer reducer;

	public JtsAdapter() {
		gf = new GeometryFactory(new PrecisionModel(100));
		reducer = new GeometryPrecisionReducer(gf.getPrecisionModel());
		reducer.setChangePrecisionModel(true);
		imageTree = new Quadtree();
	}

	@Override
	public void beginObject(InputPosition pos) {
		currentObject = new ArrayList<>();
	}

	@Override
	public void endPath(InputPosition pos, Exposure exposure) {
		currentPath.closePath();
//		printCurrentPath();
		Geometry currentPathGeometry = reducer.reduce(ShapeReader.read(currentPath.getPathIterator(null, 0.01), gf));
		if (exposure == Exposure.ON) {
			currentObject.add(currentPathGeometry);
		} else {
			Geometry tmp = gf.createGeometryCollection(currentObject.toArray(new Geometry[] {})).union()
					.difference(currentPathGeometry);
			currentObject = new ArrayList<>();
			currentObject.add(tmp);
		}
	}

	private void printCurrentPath() {
		System.out.println("endPath");
		{
			double[] pathPt = new double[6];
			var pathIt = currentPath.getPathIterator(null, 0.01);
			while (!pathIt.isDone()) {
				int segType = pathIt.currentSegment(pathPt);
				switch (segType) {
				case PathIterator.SEG_MOVETO:
					System.out.print("move ");
					break;
				case PathIterator.SEG_LINETO:
					System.out.print("line ");
					break;
				case PathIterator.SEG_CLOSE:
					System.out.print("close ");
					break;
				}
				System.out.println(new Coordinate(pathPt[0], pathPt[1]));
				pathIt.next();
			}
		}
	}

	private static class GeometryRef {
		public Geometry geometry;

		public GeometryRef(Geometry geometry) {
			this.geometry = geometry;
		}
	}

	@Override
	public void endObject(InputPosition pos, Polarity polarity) {
		if (currentObject != null) {
			if (polarity == Polarity.DARK) {
				for (var g : currentObject) {
					if (g.isEmpty())
						continue;
					try {
						imageTree.insert(g.getEnvelopeInternal(), new GeometryRef(g));
					} catch (Exception e) {
						throw new RuntimeException("Error wile inserting " + g, e);
					}
				}
			} else {
				for (var g : currentObject) {
					if (g.isEmpty())
						continue;
					for (var refObj : imageTree.query(g.getEnvelopeInternal())) {
						GeometryRef ref = (GeometryRef) refObj;
						ref.geometry = ref.geometry.difference(g);
					}
				}
			}
			currentObject = null;
		}
	}

	public Geometry image() {
		List<Geometry> geometries = new ArrayList<>();
		for (var refObj : imageTree.queryAll()) {
			GeometryRef ref = (GeometryRef) refObj;
			geometries.add(ref.geometry);
		}
		GeometryCollection collection = gf.createGeometryCollection(geometries.toArray(new Geometry[] {}));
		return OverlayNGRobust.union(collection);
	}

}
