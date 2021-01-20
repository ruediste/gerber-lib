package com.github.ruediste.gerberLib.jts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.jts.MoveGenerator.LineStringWrapper;
import com.github.ruediste.gerberLib.jts.MoveGenerator.TreeItem;

public class MoveGeneratorTest {

	private MoveHandler handler;
	private MoveGenerator generator;

	@BeforeEach
	public void before() {
		handler = mock(MoveHandler.class);
		generator = new MoveGenerator(new WarningCollector(), handler);
	}

//	@Test
	public void generateMoves() throws Exception {
		var gf = new GeometryFactory();
		CoordinateArraySequence seq1 = new CoordinateArraySequence(new Coordinate[] { new Coordinate(0, 0),
				new Coordinate(1, 0), new Coordinate(0, 1), new Coordinate(0, 0) }, 2);
		Polygon poly1 = new Polygon(new LinearRing(seq1, gf), null, gf);

		generator.add(poly1);
		generator.generateMoves(new Coordinate(-1, -1));
		var o = inOrder(handler);
		o.verify(handler).moveTo(new Coordinate(-1, -1));
		o.verify(handler).moveTo(seq1.getCoordinate(0));
		for (int i = 1; i <= 3; i++)
			o.verify(handler).moveTo(seq1.getCoordinate(i));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void processLineStringClosed() throws Exception {
		LineStringWrapper wrapper = makeWrapper(true);

		Consumer<TreeItem> consumer = mock(Consumer.class);
		Consumer<TreeItem> remove = mock(Consumer.class);
		generator.processLineString(wrapper.items.get(0), consumer, remove);
		var o = inOrder(consumer, remove);
		o.verify(consumer).accept(wrapper.items.get(1));
		o.verify(remove).accept(wrapper.items.get(1));
		o.verify(consumer).accept(wrapper.items.get(2));
		o.verify(remove).accept(wrapper.items.get(2));
		o.verify(consumer).accept(wrapper.items.get(0));
		o.verify(remove).accept(wrapper.items.get(0));
		o.verifyNoMoreInteractions();
		reset(consumer, remove);

		generator.processLineString(wrapper.items.get(1), consumer, remove);
		o.verify(consumer).accept(wrapper.items.get(2));
		o.verify(remove).accept(wrapper.items.get(2));
		o.verify(consumer).accept(wrapper.items.get(0));
		o.verify(remove).accept(wrapper.items.get(0));
		o.verify(consumer).accept(wrapper.items.get(1));
		o.verify(remove).accept(wrapper.items.get(1));
		o.verifyNoMoreInteractions();
		reset(consumer, remove);

		generator.processLineString(wrapper.items.get(2), consumer, remove);
		o.verify(consumer).accept(wrapper.items.get(0));
		o.verify(remove).accept(wrapper.items.get(0));
		o.verify(consumer).accept(wrapper.items.get(1));
		o.verify(remove).accept(wrapper.items.get(1));
		o.verify(consumer).accept(wrapper.items.get(2));
		o.verify(remove).accept(wrapper.items.get(2));
		o.verifyNoMoreInteractions();
		reset(consumer, remove);

	}

	private LineStringWrapper makeWrapper(boolean closed) {
		LineStringWrapper wrapper = new LineStringWrapper();
		wrapper.isClosed = closed;
		new TreeItem(null, 0, wrapper);
		new TreeItem(null, 1, wrapper);
		new TreeItem(null, 2, wrapper);
		if (!closed)
			new TreeItem(null, 3, wrapper);
		wrapper.max = wrapper.items.size() - 1;
		return wrapper;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void processLineStringOpen() throws Exception {
		LineStringWrapper wrapper;
		Consumer<TreeItem> consumer = mock(Consumer.class);
		Consumer<TreeItem> remove = mock(Consumer.class);
		var o = inOrder(consumer, remove);

		wrapper = makeWrapper(false);
		generator.processLineString(wrapper.items.get(0), consumer, remove);
		assertEquals(0, wrapper.min);
		assertEquals(0, wrapper.max);
		o.verify(remove).accept(wrapper.items.get(0));
		o.verify(consumer).accept(wrapper.items.get(1));
		o.verify(remove).accept(wrapper.items.get(1));
		o.verify(consumer).accept(wrapper.items.get(2));
		o.verify(remove).accept(wrapper.items.get(2));
		o.verify(consumer).accept(wrapper.items.get(3));
		o.verify(remove).accept(wrapper.items.get(3));
		o.verifyNoMoreInteractions();
		reset(consumer, remove);

		wrapper = makeWrapper(false);
		generator.processLineString(wrapper.items.get(1), consumer, remove);
		assertEquals(0, wrapper.min);
		assertEquals(1, wrapper.max);
		o.verify(consumer).accept(wrapper.items.get(2));
		o.verify(remove).accept(wrapper.items.get(2));
		o.verify(consumer).accept(wrapper.items.get(3));
		o.verify(remove).accept(wrapper.items.get(3));
		o.verifyNoMoreInteractions();

		generator.processLineString(wrapper.items.get(1), consumer, remove);
		o.verify(remove).accept(wrapper.items.get(1));
		o.verify(consumer).accept(wrapper.items.get(0));
		o.verify(remove).accept(wrapper.items.get(0));
		o.verifyNoMoreInteractions();
		reset(consumer, remove);

		wrapper = makeWrapper(false);
		generator.processLineString(wrapper.items.get(3), consumer, remove);
		assertEquals(3, wrapper.min);
		assertEquals(3, wrapper.max);
		o.verify(remove).accept(wrapper.items.get(3));
		o.verify(consumer).accept(wrapper.items.get(2));
		o.verify(remove).accept(wrapper.items.get(2));
		o.verify(consumer).accept(wrapper.items.get(1));
		o.verify(remove).accept(wrapper.items.get(1));
		o.verify(consumer).accept(wrapper.items.get(0));
		o.verify(remove).accept(wrapper.items.get(0));
		o.verifyNoMoreInteractions();

	}

}
