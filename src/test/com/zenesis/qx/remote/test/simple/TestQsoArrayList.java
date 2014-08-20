package com.zenesis.qx.remote.test.simple;

import junit.framework.TestCase;

import com.zenesis.qx.event.Event;
import com.zenesis.qx.event.EventListener;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.ArrayList;
import com.zenesis.qx.remote.collections.ArrayList.ChangeData;
import com.zenesis.qx.remote.collections.ArrayList.Type;

public class TestQsoArrayList extends TestCase {

	@Property
	private ArrayList<String> array = new ArrayList<String>();
	private static final class Listener implements EventListener {
		
		private ArrayList<ChangeData> events = new ArrayList<ArrayList.ChangeData>();
		
		@Override
		public void handleEvent(Event event) {
			events.add((ChangeData)event.getData());
		}
		
		public void assertAdded(String value) {
			ChangeData data = events.remove(0);
			assertTrue(data.type == Type.ADD);
			assertEquals(data.added.length, 1);
			assertEquals(data.removed.length, 0);
			assertEquals(data.added[0], value);
		}
		
		public void assertAdded(String[] values) {
			ChangeData data = events.remove(0);
			assertTrue(data.type == Type.ADD);
			assertEquals(values.length, data.added.length);
			assertEquals(0, data.removed.length);
			for (int i = 0; i < values.length; i++)
				assertEquals(values[i], data.added[i]);
		}
		
		public void assertRemoved(String value) {
			ChangeData data = events.remove(0);
			assertTrue(data.type == Type.REMOVE);
			assertEquals(data.added.length, 0);
			assertEquals(data.removed.length, 1);
			assertEquals(data.removed[0], value);
		}
		
		public void assertRemoved(String[] values) {
			ChangeData data = events.remove(0);
			assertTrue(data.type == Type.REMOVE);
			assertEquals(0, data.added.length);
			assertEquals(values.length, data.removed.length);
			for (int i = 0; i < data.removed.length; i++)
				assertEquals(values[i], data.removed[i]);
		}
		
		public void assertChanged(String value, String oldValue, int index) {
			ChangeData data = events.remove(0);
			assertEquals(Type.ADD_REMOVE, data.type);
			assertEquals(1, data.added.length);
			assertEquals(value, data.added[0]);
			assertEquals(1, data.removed.length);
			assertEquals(oldValue, data.removed[0]);
			assertEquals(index, data.start);
		}
		
		public void assertEmpty() {
			assertTrue(events.isEmpty());
		}
	}
	private Listener listener = new Listener();
	
	public TestQsoArrayList() {
		EventManager.addListener(array, "change", listener);
	}
	
	public ArrayList<String> getArray() {
		return array;
	}
	
	public void testArray() {
		
		array.add("alpha");
		listener.assertAdded("alpha");
		array.add("bravo");
		listener.assertAdded("bravo");
		array.add("charlie");
		listener.assertAdded("charlie");
		listener.assertEmpty();
		
		array.set(1, "delta");
		listener.assertChanged("delta", "bravo", 1);

		array.remove(1);
		listener.assertRemoved("delta");
		listener.assertEmpty();
		
		ArrayList<String> tmp;
		tmp = new ArrayList<String>();
		tmp.add("foxtrot");
		tmp.add("echo");
		
		array.addAll(tmp);
		listener.assertAdded(new String[] { "foxtrot", "echo" });
		
		array.removeAll(tmp);
		listener.assertRemoved(new String[] { "foxtrot", "echo" });
		
		array.addAll(tmp);
		listener.assertAdded(new String[] { "foxtrot", "echo" });
		
		tmp = new ArrayList<String>();
		tmp.add("foxtrot");
		tmp.add("charlie");
		array.retainAll(tmp);
		listener.assertRemoved(new String[] { "alpha", "echo" });
		assertEquals(2, array.size());
		assertEquals("charlie", array.get(0));
		assertEquals("foxtrot", array.get(1));
	}
	
}
