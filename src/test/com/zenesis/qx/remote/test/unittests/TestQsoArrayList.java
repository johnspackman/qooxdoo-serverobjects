package com.zenesis.qx.remote.test.unittests;

import junit.framework.TestCase;

import com.zenesis.qx.event.Event;
import com.zenesis.qx.event.EventListener;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.ArrayList;
import com.zenesis.qx.remote.collections.ArrayList.ArrayChangeData;

public class TestQsoArrayList extends TestCase {

	@Property
	private ArrayList<String> array = new ArrayList<String>();
	private static final class Listener implements EventListener {
		
		private ArrayList<ArrayChangeData> events = new ArrayList<ArrayChangeData>();
		
		@Override
		public void handleEvent(Event event) {
			events.add((ArrayChangeData)event.getData());
		}
		
		public void assertAdded(String value) {
			ArrayChangeData data = events.remove(0);
			assertEquals(data.added.size(), 1);
			assertEquals(data.removed, null);
			assertEquals(data.added.get(0), value);
		}
		
		public void assertAdded(String[] values) {
			ArrayChangeData data = events.remove(0);
			assertEquals(values.length, data.added.size());
			assertEquals(null, data.removed);
			for (int i = 0; i < values.length; i++)
				assertEquals(values[i], data.added.get(i));
		}
		
		public void assertRemoved(String value) {
			ArrayChangeData data = events.remove(0);
			assertEquals(data.added, null);
			assertEquals(data.removed.size(), 1);
			assertEquals(data.removed.get(0), value);
		}
		
		public void assertRemoved(String[] values) {
			ArrayChangeData data = events.remove(0);
			assertEquals(null, data.added);
			assertEquals(values.length, data.removed.size());
			for (int i = 0; i < data.removed.size(); i++)
				assertEquals(values[i], data.removed.get(i));
		}
		
		public void assertChanged(String value, String oldValue, int index) {
			ArrayChangeData data = events.remove(0);
			assertEquals(1, data.added.size());
			assertEquals(value, data.added.get(0));
			assertEquals(1, data.removed.size());
			assertEquals(oldValue, data.removed.get(0));
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
