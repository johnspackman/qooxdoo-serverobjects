/**
 * ************************************************************************
 * 
 *    server-objects - a contrib to the Qooxdoo project that makes server 
 *    and client objects operate seamlessly; like Qooxdoo, server objects 
 *    have properties, events, and methods all of which can be access from
 *    either server or client, regardless of where the original object was
 *    created.
 * 
 *    http://qooxdoo.org
 * 
 *    Copyright:
 *      2010 Zenesis Limited, http://www.zenesis.com
 * 
 *    License:
 *      LGPL: http://www.gnu.org/licenses/lgpl.html
 *      EPL: http://www.eclipse.org/org/documents/epl-v10.php
 *      
 *      This software is provided under the same licensing terms as Qooxdoo,
 *      please see the LICENSE file in the Qooxdoo project's top-level directory 
 *      for details.
 * 
 *    Authors:
 *      * John Spackman (john.spackman@zenesis.com)
 * 
 * ************************************************************************
 */
package com.zenesis.qx.event.test;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.zenesis.qx.event.Event;
import com.zenesis.qx.event.EventListener;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.event.EventVerifiable;
import com.zenesis.qx.event.Eventable;
import com.zenesis.qx.remote.collections.ArrayList;
import com.zenesis.qx.remote.collections.ArrayList.ArrayChangeData;
import com.zenesis.qx.test.AbstractTestCase;

public class TestEventManager extends AbstractTestCase {
	
	private static final class MyEventListener implements EventListener {
		private final PrintWriter out;
		private final int index;
		
		public MyEventListener(PrintWriter out, int index) {
			super();
			this.out = out;
			this.index = index;
		}

		@Override
		public void handleEvent(Event event) {
			out.println("Listener #" + index + ": " + event + " on " + event.getCurrentTarget() + ", data=" + event.getData());
		}
	};

	/**
	 * Adds between 0 and 9 listeners to each of 10 objects; this tests the EventManagers
	 * ability to record and trigger listeners where it is storing the listener directly,
	 * as an array, and as HashMaps and LinkedHashSets
	 * @throws Exception
	 */
	public void test1() throws Exception {
		new EventManager();
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		String[] testObjects = new String[10];
		for (int i = 0; i < testObjects.length; i++)
			testObjects[i] = new String(Integer.toString(i));
		
		for (int i = 0; i < testObjects.length; i++) {
			EventListener listener = new MyEventListener(pw, i);
			for (int j = 1; j <= i; j++) {
				EventManager.addListener(testObjects[j], "helloWorldEvent", listener);
			}
		}
		
		for (int i = 0; i < testObjects.length; i++) {
			EventManager.fireDataEvent(testObjects[i], "helloWorldEvent", i);
		}
		
		assertFromFile(sw.toString(), "TestEventManager.test1");
	}
	
	/**
	 * Tests the EventManager's removeListener
	 * @throws Exception
	 */
	public void test2() throws Exception {
		EventManager mgr = new EventManager();
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		String[] testObjects = new String[10];
		for (int i = 0; i < testObjects.length; i++)
			testObjects[i] = new String(Integer.toString(i));
		
		EventListener[] listeners = new EventListener[testObjects.length];
		for (int i = 0; i < listeners.length; i++)
			listeners[i] = new MyEventListener(pw, i);
		
		EventManager.addListener(testObjects[0], "myEvent0", listeners[0]);
		EventManager.addListener(testObjects[0], "myEvent1", listeners[1]);
		EventManager.addListener(testObjects[0], "myEvent2", listeners[2]);
		EventManager.addListener(testObjects[0], "myEvent3", listeners[3]);
		EventManager.addListener(testObjects[0], "myEvent4", listeners[4]);
		EventManager.addListener(testObjects[0], "myEvent5", listeners[5]);
		EventManager.removeListener(testObjects[0], "myEvent5", listeners[5]);
		EventManager.removeListener(testObjects[0], "myEvent4", listeners[4]);
		EventManager.removeListener(testObjects[0], "myEvent3", listeners[3]);
		EventManager.removeListener(testObjects[0], "myEvent2", listeners[2]);
		EventManager.removeListener(testObjects[0], "myEvent1", listeners[1]);
		EventManager.removeListener(testObjects[0], "myEvent0", listeners[0]);
		assertTrue(mgr.compact());
		
		for (int i = 0; i < testObjects.length; i++)
			for (int j = 1; j <= i; j++)
				EventManager.addListener(testObjects[j], "helloWorldEvent" + i, listeners[j]);
		
		for (int i = 0; i < testObjects.length; i++)
			for (int j = 1; j <= i; j++)
				assertTrue(EventManager.removeListener(testObjects[j], "helloWorldEvent" + i, listeners[j]));
		
		assertTrue(mgr.compact());
	}
	
	/**
	 * Tests validation
	 * @throws Exception
	 */
	public void test3() throws Exception {
		new EventManager();
		TestObject testObject = new TestObject();
		
		EventListener listener = new MyEventListener(null, 0);
		EventManager.addListener(testObject, "helloWorld", listener);
		try {
			EventManager.addListener(testObject, "helloWorld", listener);
			assertTrue(false);
		} catch(IllegalArgumentException e) {
			// Nothing - test passed
		}
		
		assertFalse(EventManager.addListener(testObject, "noSuchEvent", listener));
	}
	
	private class OnEventChangeListener implements EventListener {
        int numAddedEvents = 0;
        int numRemovedEvents = 0;
        
        @Override
        public void handleEvent(com.zenesis.qx.event.Event changeEvt) {
            ArrayChangeData data = (ArrayChangeData)changeEvt.getData();
            if (data.removed != null) {
                for (Object obj : data.removed) {
                    numRemovedEvents++;
                }
            }
            if (data.added != null) {
                for (Object obj : data.added) {
                    numAddedEvents++;
                }
            }
        }
	}
	
	public void test4() {
	    
	    OnEventChangeListener ON_EVENTS_CHANGE = new OnEventChangeListener();

	    ArrayList<String> stringArray = new ArrayList();
        int size = EventManager.getInstance().size();
        EventManager.addListener(stringArray, "change", ON_EVENTS_CHANGE);
        assert(size == EventManager.getInstance().size());
        stringArray.add("alpha");
        assert(ON_EVENTS_CHANGE.numAddedEvents == 1);
        assert(ON_EVENTS_CHANGE.numRemovedEvents == 0);
        stringArray.add("bravo");
        stringArray.add("charlie");
        stringArray.add("delta");
        stringArray.add("echo");
        assert(size == EventManager.getInstance().size());
        assert(ON_EVENTS_CHANGE.numAddedEvents == 5);
        assert(ON_EVENTS_CHANGE.numRemovedEvents == 0);
        stringArray.remove("charlie");
        assert(ON_EVENTS_CHANGE.numAddedEvents == 5);
        assert(ON_EVENTS_CHANGE.numRemovedEvents == 1);
        EventManager.removeListener(stringArray, "change", ON_EVENTS_CHANGE);
        assert(size == EventManager.getInstance().size());
	}
}

class TestObject implements EventVerifiable {

	/* (non-Javadoc)
	 * @see com.zenesis.qx.event.Eventable#supportsEvent(java.lang.String)
	 */
	@Override
	public boolean supportsEvent(String eventName) {
		return eventName.equals("helloWorld");
	}
	
}
