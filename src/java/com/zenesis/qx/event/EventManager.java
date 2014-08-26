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
package com.zenesis.qx.event;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

/**
 * Manages events and listeners on arbitrary objects.
 * 
 * The listeners are stored as values in a WeakHashMap where the key is
 * the object to listen to.  The value of the map is either: a) an instance
 * of NamedEventListener, b) a small array of NamedEventListeners, or c) a 
 * HashMap where the key is the name of the event and the value is the listener.
 * 
 * The NamedEventListener's listener object is either a small array of 
 * EventListener's or a LinkedHashSet of EventListeners.
 * 
 * The reason for using Objects and reflection instead of always using HashMap
 * and LinkedHashSet is to keep the overhead down and support the common use case
 * where an object will have only a few events listened to, and each event
 * will only be listened to by a single piece of code (this is pure hypothesis!).
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
public class EventManager {
	
	private static final Logger log = Logger.getLogger(EventManager.class);
	
	private static final int TINY_ARRAY_SIZE = 5;
	
	/*
	 * Links an event name with a listener; the listener is actually either a) null,
	 * b) an EventListener, c) and array of EventListeners, or d) a ArrayList of EventListeners.
	 */
	private static final class NamedEventListener {
		public final String eventName;
		public Object listener;

		public NamedEventListener(String eventName, EventListener listener) {
			super();
			this.eventName = eventName;
			this.listener = listener;
		}
		
		/**
		 * Adds a listener
		 * @param newListener
		 * @throws IllegalArgumentException if the listener is added twice
		 */
		public void addListener(EventListener newListener) throws IllegalArgumentException{
			// Nothing so far? Easy.
			if (listener == null) {
				listener = newListener;
				return;
			}
			
			final Class clazz = listener.getClass();
			
			// It's an array - try and use an empty slot
			if (clazz.isArray()) {
				EventListener[] list = (EventListener[])listener;
				for (int i = 0; i < TINY_ARRAY_SIZE; i++)
					if (list[i] == newListener)
						throw new IllegalArgumentException("Cannot add the same listener to the same object and eventName twice");
				
				for (int i = 0; i < TINY_ARRAY_SIZE; i++) {
					// Free slot?  Use it
					if (list[i] == null) {
						list[i] = newListener;
						return;
					}
				}
				
				// No room so upgrade to a ArrayList
				ArrayList<EventListener> set = new ArrayList<EventListener>();
				for (int i = 0; i < TINY_ARRAY_SIZE; i++)
					set.add(list[i]);
				set.add(newListener);
				listener = set;
				
			// Already a ArrayList - add to it
			} else if (clazz == ArrayList.class) {
				ArrayList<EventListener> set = (ArrayList<EventListener>)listener;
				int pos = -1;
				for (int i = 0;i < set.size(); i++) {
					EventListener tmp = set.get(i);
					if (tmp == null && pos == -1)
						pos = i;
					else if (tmp == newListener)
						throw new IllegalArgumentException("Cannot add the same listener to the same object and eventName twice");
				}
				if (pos != -1)
					set.set(pos, newListener);
				else
					set.add(newListener);
				
			// Must be an EventListener instance, convert to an array 
			} else {
				assert(clazz == EventListener.class);
				if (listener == newListener)
					throw new IllegalArgumentException("Cannot add the same listener to the same object and eventName twice");
				EventListener[] list = new EventListener[TINY_ARRAY_SIZE];
				list[0] = (EventListener)listener;
				list[1] = newListener;
				this.listener = list;
			}
		}
		
		/**
		 * Removes a listener
		 * @param oldListener
		 * @return true if the listener existed and was removed
		 */
		public boolean removeListener(EventListener oldListener) {
			if (listener == null)
				return false;
			
			final Class clazz = listener.getClass();
			
			// It's an array - find it and remove it
			if (clazz.isArray()) {
				EventListener[] list = (EventListener[])listener;
				for (int i = 0; i < TINY_ARRAY_SIZE; i++) {
					if (list[i] == oldListener) {
						list[i] = null;
						return true;
					}
				}
				return false;
			}
				
			// Already a ArrayList
			if (clazz == ArrayList.class) {
				ArrayList<EventListener> set = (ArrayList<EventListener>)listener;
				for (int i = 0; i < set.size(); i++) {
					EventListener tmp = set.get(i);
					if (tmp == oldListener) {
						set.set(i, null);
						return true;
					}
				}
				return false;
			}
				
			// Must be an EventListener instance, convert to an array 
			assert(clazz == EventListener.class);
			if (listener != oldListener)
				return false;
			
			listener = null;
			return true;
		}
		
		/**
		 * Removes a listener
		 * @param oldListener
		 * @return true if the listener existed and was removed
		 */
		public boolean hasListener(EventListener oldListener) {
			if (listener == null)
				return false;
			
			final Class clazz = listener.getClass();
			
			// It's an array - find it and remove it
			if (clazz.isArray()) {
				EventListener[] list = (EventListener[])listener;
				for (int i = 0; i < TINY_ARRAY_SIZE; i++)
					if (list[i] == oldListener)
						return true;
				return false;
			}
				
			// Already a ArrayList
			if (clazz == ArrayList.class) {
				ArrayList<EventListener> set = (ArrayList<EventListener>)listener;
				for (EventListener tmp : set)
					if (tmp == oldListener)
						return true;
				return false;
			}
				
			// Must be an EventListener instance 
			assert(clazz == EventListener.class);
			if (listener != oldListener)
				return false;
			return true;
		}

		/**
		 * Fires an event on the listener(s)
		 */
		public void fireEvent(Event event) {
			if (listener == null)
				return;
			
			final Class clazz = listener.getClass();
			
			// It's an array - find it and remove it
			if (clazz.isArray()) {
				EventListener[] list = (EventListener[])listener;
				for (int i = 0; i < TINY_ARRAY_SIZE; i++)
					if (list[i] != null) {
						EventListener listener = list[i];
						if (listener != null)
							listener.handleEvent(event);
					}
				return;
			}
				
			// Already a ArrayList
			if (clazz == ArrayList.class) {
				ArrayList<EventListener> set = (ArrayList<EventListener>)listener;
				for (EventListener listener : set) {
					if (listener != null)
						listener.handleEvent(event);
				}
				return;
			}
				
			// Must be an EventListener instance, convert to an array 
			assert(clazz == EventListener.class);
			EventListener listener = (EventListener)this.listener;
			if (listener != null)
				listener.handleEvent(event);
		}
		
		public boolean isEmpty() {
			if (listener == null)
				return true;
			
			final Class clazz = listener.getClass();
			
			// It's an array
			if (clazz.isArray()) {
				EventListener[] list = (EventListener[])listener;
				for (int i = 0; i < TINY_ARRAY_SIZE; i++)
					if (list[i] != null)
						return false;
				return true;
			}
				
			// Already a ArrayList
			if (clazz == ArrayList.class) {
				ArrayList<EventListener> set = (ArrayList<EventListener>)listener;
				for (EventListener ref : set)
					if (ref != null)
						return false;
				return true;
			}
				
			// Must be an EventListener instance 
			assert(clazz == EventListener.class);
			return listener == null;
		}
	}
	
	private static EventManager s_instance;
	
	private WeakHashMap<Object, Object> listeners = new WeakHashMap<Object, Object>();

	/**
	 * Constructor; the first instance of EventManager will become the global default
	 */
	public EventManager() {
		this(true);
	}

	/**
	 * Constructor; the first instance of EventManager will become the global default only
	 * if setGlobal is true
	 * @param setGlobal whether to make this instance the global one
	 */
	public EventManager(boolean setGlobal) {
		super();
		if (setGlobal) {
			if (s_instance != null)
				log.warn("Replacing global instance of EventManager");
			s_instance = this;
		}
	}

	/**
	 * Adds an event listener
	 * @param keyObject
	 * @param eventName
	 * @param listener
	 * @throws {@link IllegalArgumentException} if a listener is added twice
	 * @return true if the event was added
	 */
	public static boolean addListener(Object keyObject, String eventName, EventListener listener) throws IllegalArgumentException{
		return getInstance()._addListener(keyObject, eventName, listener);
	}

	/**
	 * Adds an event listener
	 * @param keyObject
	 * @param eventName
	 * @param listener
	 * @throws {@link IllegalArgumentException} if a listener is added twice
	 * @return true if the event was added
	 */
	protected synchronized boolean _addListener(Object keyObject, String eventName, EventListener listener) throws IllegalArgumentException{
		if (!supportsEvent(keyObject, eventName))
			return false;
		
		Object identityObject = getIdentity(keyObject);
		Object current = listeners.get(identityObject);
		
		// If the object is not yet known, then create a new NEL and return
		if (current == null) {
			NamedEventListener nel = new NamedEventListener(eventName, listener);
			listeners.put(identityObject, nel);
			return true;
		}
		
		final Class clazz = current.getClass();
		
		// A NamedEventListener?  Then we've found it
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)current;
			if (nel.eventName.equals(eventName))
				nel.addListener(listener);
			else {
				NamedEventListener[] nels = new NamedEventListener[TINY_ARRAY_SIZE];
				nels[0] = nel;
				nels[1] = new NamedEventListener(eventName, listener);
				listeners.put(identityObject, nels);
			}
			return true;
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])current;
			
			// Look for a NamedEventListener for the eventName, or a free slot in the array
			int index = 0;
			int freeIndex = -1;
			while (index < TINY_ARRAY_SIZE) {
				if (nels[index] == null) {
					if (freeIndex < 0)
						freeIndex = index;
				} else if (nels[index].eventName.equals(eventName))
					break;
				index++;
			}
			
			// Found a NamedEventListener?
			if (index < TINY_ARRAY_SIZE) {
				nels[index].addListener(listener);
			
			// Found a free slot?
			} else if (freeIndex > -1) {
				NamedEventListener nel = nels[freeIndex] = new NamedEventListener(eventName, null);
				nel.addListener(listener);
			
			// The array is full - convert to a HashMap
			} else {
				// Convert to a map
				HashMap<String, NamedEventListener> map = new HashMap<String, NamedEventListener>();
				for (int i = 0; i < TINY_ARRAY_SIZE; i++)
					map.put(nels[i].eventName, nels[i]);
				
				// Add the new NamedEventListener
				map.put(eventName, new NamedEventListener(eventName, listener));
				
				// Replace the array with the map
				listeners.put(identityObject, map);
			}
			
			return true;
		}
		
		// By elimination, it must be a HashMap
		assert(current.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)current;
		NamedEventListener nel = map.get(eventName);
		if (nel == null)
			map.put(eventName, new NamedEventListener(eventName, listener));
		else
			nel.addListener(listener);
		
		return true;
	}
	
	/**
	 * Removes a listener from an object and eventName
	 * @param keyObject
	 * @param eventName
	 * @param listener
	 * @return
	 */
	public static boolean removeListener(Object keyObject, String eventName, EventListener listener) {
		return getInstance()._removeListener(keyObject, eventName, listener);
	}
	
	/**
	 * Removes a listener from an object and eventName
	 * @param keyObject
	 * @param eventName
	 * @param listener
	 * @return
	 */
	protected synchronized boolean _removeListener(Object keyObject, String eventName, EventListener listener) {
		if (listener.toString().equals("/aaa/CLS Implants BioHorizons.mov"))
			listener = listener;
		Object identityObject = getIdentity(keyObject);
		if (eventName == null && listener == null)
			return listeners.remove(identityObject) != null;
		
		Object current = listeners.get(identityObject);
		if (current == null)
			return false;
		
		final Class clazz = current.getClass();
		
		// A NamedEventListener?  Then check the eventName 
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)current;
			if (eventName != null && !nel.eventName.equals(eventName))
				return false;
			if (listener == null) {
				listeners.remove(identityObject);
				return true;
			}
			return nel.removeListener(listener);
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])current;
			boolean removed = false;
			
			// Look for a NamedEventListener for the eventName
			for (int i = 0; i < TINY_ARRAY_SIZE; i++)
				if (nels[i] != null)
					if (eventName == null || nels[i].eventName.equals(eventName)) {
						if (listener == null) {
							nels[i] = null;
							removed = true;
						} else if (nels[i].removeListener(listener))
							removed = true;
					}
			
			return removed;
		}
		
		// By elimination, it must be a HashMap
		assert(current.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)current;
		if (eventName == null) {
			boolean removed = false;
			for (NamedEventListener nel : map.values())
				if (nel.removeListener(listener))
					removed = true;
			return removed;
		}
		if (listener == null)
			return map.remove(eventName) != null;
		
		NamedEventListener nel = map.get(eventName);
		if (nel == null)
			return false;
		return nel.removeListener(listener);
	}
	
	/**
	 * Removes a listener from an object and eventName
	 * @param keyObject
	 * @param eventName
	 * @param listener
	 * @return
	 */
	public static boolean hasListener(Object keyObject, String eventName, EventListener listener) {
		return getInstance()._hasListener(keyObject, eventName, listener);
	}
	
	/**
	 * Removes a listener from an object and eventName
	 * @param keyObject
	 * @param eventName
	 * @param listener
	 * @return
	 */
	protected synchronized boolean _hasListener(Object keyObject, String eventName, EventListener listener) {
		Object current = listeners.get(getIdentity(keyObject));
		if (current == null)
			return false;
		
		final Class clazz = current.getClass();
		
		// A NamedEventListener?  Then check the eventName 
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)current;
			if (!nel.eventName.equals(eventName))
				return false;
			return nel.hasListener(listener);
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])current;
			
			// Look for a NamedEventListener for the eventName
			for (int i = 0; i < TINY_ARRAY_SIZE; i++)
				if (nels[i].eventName.equals(eventName))
					return nels[i].hasListener(listener);
			
			return false;
		}
		
		// By elimination, it must be a HashMap
		assert(current.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)current;
		NamedEventListener nel = map.get(eventName);
		if (nel == null)
			return false;
		return nel.hasListener(listener);
	}
	
	/**
	 * Detects whether the object supports the given event name; by default, all objects
	 * are considered to support events.
	 * @param obj
	 * @param eventName
	 * @return
	 */
	public static boolean supportsEvent(Object obj, String eventName) {
		return getInstance()._supportsEvent(obj, eventName);
	}
	
	/**
	 * Detects whether the object supports the given event name; by default, all objects
	 * are considered to support events.
	 * @param obj
	 * @param eventName
	 * @return
	 */
	protected boolean _supportsEvent(Object obj, String eventName) {
		if (obj instanceof Eventable) {
			Eventable ev = (Eventable)obj;
			return ev.supportsEvent(eventName);
		}
		
		return true;
	}

	/**
	 * Fires an event on the object
	 * @param obj
	 * @param eventName
	 */
	public static void fireEvent(Object keyObject, String eventName) {
		getInstance().fireDataEvent(new Event(keyObject, keyObject, eventName, null));
	}

	/**
	 * Fires a data event on the object
	 * @param obj
	 * @param eventName
	 * @param data
	 */
	public static void fireDataEvent(Object keyObject, String eventName, Object data) {
		getInstance().fireDataEvent(new Event(keyObject, keyObject, eventName, data));
	}

	/**
	 * Fires a data event on the object
	 * @param obj
	 * @param eventName
	 * @param data
	 */
	public void fireDataEvent(Event event) {
		Object current = listeners.get(getIdentity(event.getOriginalTarget()));
		if (current == null)
			return;
		
		final Class clazz = current.getClass();
		
		// A NamedEventListener?  Then check the eventName 
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)current;
			if (!nel.eventName.equals(event.getEventName()))
				return;
			nel.fireEvent(event);
			return;
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])current;
			
			// Look for a NamedEventListener for the eventName
			for (int i = 0; i < TINY_ARRAY_SIZE; i++)
				if (nels[i] != null && nels[i].eventName.equals(event.getEventName()))
					nels[i].fireEvent(event);
			
			return;
		}
		
		// By elimination, it must be a HashMap
		assert(current.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)current;
		NamedEventListener nel = map.get(event.getEventName());
		if (nel == null)
			return;
		nel.fireEvent(event);
	}

	/**
	 * Compacts the lists, removing everything that has empty references and returns whether
	 * there are any event listeners on any object
	 * @return
	 */
	public synchronized boolean compact() {
		Map.Entry[] values = listeners.entrySet().toArray(new Map.Entry[listeners.entrySet().size()]);
		for (int i = 0; i < values.length; i++) {
			Object current = values[i].getValue();
			final Class clazz = current.getClass();
			
			// A NamedEventListener?  Then check the eventName 
			if (clazz == NamedEventListener.class) {
				NamedEventListener nel = (NamedEventListener)current;
				if (nel.isEmpty())
					current = null;
			}

			// If there is an array, it's an array of NamedEventListeners
			else if (clazz.isArray()){
				NamedEventListener[] nels = (NamedEventListener[])current;
				boolean nelsEmpty = true;
				
				// Look for a NamedEventListener for the eventName
				for (int j = 0; j < TINY_ARRAY_SIZE; j++)
					if (nels[j] != null) {
						if (nels[j].isEmpty())
							nels[j] = null;
						else
							nelsEmpty = false;
					}
				if (nelsEmpty)
					current = null;
				
			// By elimination, it must be a HashMap
			} else {
				assert(current.getClass() == HashMap.class);
				HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)current;
				Object [] onels = map.values().toArray();
				for (int j = 0; j < onels.length; j++) {
					NamedEventListener nel = (NamedEventListener)onels[j];
					if (nel.isEmpty())
						map.remove(nel.eventName);
				}
				if (map.isEmpty())
					current = null;
			}
			if (current == null)
				listeners.remove(values[i].getKey());
		}
		values = listeners.entrySet().toArray(new Map.Entry[listeners.entrySet().size()]);
		return listeners.isEmpty();
	}
	
	/**
	 * Returns the default global instance
	 * @return
	 */
	public static EventManager getInstance() {
		if (s_instance == null)
			new EventManager(true);
		return s_instance;
	}

	private static final class IdentityObject {
		private final Object obj;
		
		IdentityObject(Object obj) {
			this.obj = obj;
		}
		
		@Override
		public int hashCode() {
			return 1;
		}

		@Override
		public boolean equals(Object that) {
			return ((IdentityObject)that).obj == this.obj;
		}
		
	}

	public static Object getIdentity(Object obj) {
		if (obj == null)
			return null;
		if (obj instanceof AbstractList)
			return new IdentityObject(obj);
		return obj;
	}
}
