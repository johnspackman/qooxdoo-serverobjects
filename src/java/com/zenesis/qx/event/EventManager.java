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

import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

/**
 * Manages events and listeners on arbitrary objects.
 * 
 * The listeners are stored as Bindings in a LinkedList where the target is
 * the object to listen to and is a WeakReference.  The listener is either: 
 * a) an instance of NamedEventListener, b) a small array of NamedEventListeners, or c) a 
 * HashMap where the key is the name of the event and the value is the listener.
 * 
 * The NamedEventListener's listener object is either a small array of 
 * EventListener's or a LinkedHashSet of EventListeners.
 * 
 * The reason for using Objects and reflection instead of always using HashMap
 * and LinkedHashSet is to keep the overhead down and support the common use case
 * where an object will have only a few events listened to, and each event
 * will only be listened to by a single piece of code (this is a guess).
 * 
 * Note that Maps cannot be used to track the bindings to a particular object
 * because that means using the target object as a key - and Maps require that
 * the hashCode for keys never changes, but List.hashCode always changes when
 * you change the contents of the list.  Instead, LinkedList is used and every
 * time the object is found it is moved half way up the list so it's found
 * faster next time.  
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
	
	/*
	 * Records the binding between a target object and the listeners; listener is either
	 * a NamedEventyListener, an array of NamedEventListeners, or a Map indexed by name
	 */
	private static final class Binding {
		public WeakReference targetRef;
		public Object listener;
		
		public Binding(Object target, Object listener) {
			super();
			this.targetRef = new WeakReference(target);
			this.listener = listener;
		}
		
		public Object getTarget() {
			return targetRef.get();
		}
	}
	
	// Linked list of bindings - note this cannot be a map because maps require the 
	//	target to be immutable (Collections change their hashCode as they are modified)
	private LinkedList<Binding> bindings = new LinkedList<Binding>();
	
	private static EventManager s_instance;
	
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

		Binding binding = getBinding(keyObject);
		
		// If the object is not yet known, then create a new NEL and return
		if (binding == null) {
			NamedEventListener nel = new NamedEventListener(eventName, listener);
			bindings.push(new Binding(keyObject, nel));
			return true;
		}
		
		final Class clazz = binding.listener.getClass();
		
		// A NamedEventListener?  Then we've found it
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)binding.listener;
			if (nel.eventName.equals(eventName))
				nel.addListener(listener);
			else {
				NamedEventListener[] nels = new NamedEventListener[TINY_ARRAY_SIZE];
				nels[0] = nel;
				nels[1] = new NamedEventListener(eventName, listener);
				bindings.push(new Binding(keyObject, nels));
			}
			return true;
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])binding.listener;
			
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
				bindings.push(new Binding(keyObject, map));
			}
			
			return true;
		}
		
		// By elimination, it must be a HashMap
		assert(binding.listener.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)binding.listener;
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
//		if (listener.toString().equals("/aaa/CLS Implants BioHorizons.mov"))
//			listener = listener;
		
		if (eventName == null && listener == null)
			return removeBinding(keyObject);
		
		Binding binding = getBinding(keyObject);
		if (binding == null)
			return false;
		
		final Class clazz = binding.listener.getClass();
		
		// A NamedEventListener?  Then check the eventName 
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)binding.listener;
			if (eventName != null && !nel.eventName.equals(eventName))
				return false;
			if (listener == null) {
				bindings.remove(binding);
				return true;
			}
			return nel.removeListener(listener);
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])binding.listener;
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
		assert(binding.listener.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)binding.listener;
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
		Binding binding = getBinding(keyObject);
		if (binding == null)
			return false;
		
		final Class clazz = binding.listener.getClass();
		
		// A NamedEventListener?  Then check the eventName 
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)binding.listener;
			if (!nel.eventName.equals(eventName))
				return false;
			return nel.hasListener(listener);
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])binding.listener;
			
			// Look for a NamedEventListener for the eventName
			for (int i = 0; i < TINY_ARRAY_SIZE; i++)
				if (nels[i].eventName.equals(eventName))
					return nels[i].hasListener(listener);
			
			return false;
		}
		
		// By elimination, it must be a HashMap
		assert(binding.listener.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)binding.listener;
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
		Binding binding = getBinding(event.getOriginalTarget());
		if (binding == null)
			return;
		
		final Class clazz = binding.listener.getClass();
		
		// A NamedEventListener?  Then check the eventName 
		if (clazz == NamedEventListener.class) {
			NamedEventListener nel = (NamedEventListener)binding.listener;
			if (!nel.eventName.equals(event.getEventName()))
				return;
			nel.fireEvent(event);
			return;
		}

		// If there is an array, it's an array of NamedEventListeners
		if (clazz.isArray()){
			NamedEventListener[] nels = (NamedEventListener[])binding.listener;
			
			// Look for a NamedEventListener for the eventName
			for (int i = 0; i < TINY_ARRAY_SIZE; i++)
				if (nels[i] != null && nels[i].eventName.equals(event.getEventName()))
					nels[i].fireEvent(event);
			
			return;
		}
		
		// By elimination, it must be a HashMap
		assert(binding.listener.getClass() == HashMap.class);
		HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)binding.listener;
		NamedEventListener nel = map.get(event.getEventName());
		if (nel == null)
			return;
		nel.fireEvent(event);
	}

	/**
	 * Finds the binding for an object, and moves it forward in the list if found
	 * @param target
	 * @return
	 */
	private Binding getBinding(Object target) {
		int index = 0;
		for (Binding bind : bindings) {
			if (bind.getTarget() == target) {
				if (index > 1) {
					bindings.remove(bind);
					bindings.add(index / 2, bind);
				}
				return bind;
			}
			index++;
		}
		return null;
	}
	
	/**
	 * Removes the binding for the target
	 * @param target
	 * @return
	 */
	private boolean removeBinding(Object target) {
		for (Binding bind : bindings)
			if (bind.getTarget() == target) {
				bindings.remove(bind);
				return true;
			}
		return false;
	}

	/**
	 * Compacts the lists, removing everything that has empty references and returns whether
	 * there are any event listeners on any object
	 * @return
	 */
	public synchronized boolean compact() {
		Binding[] values = bindings.toArray(new Binding[bindings.size()]);
		for (int i = 0; i < values.length; i++) {
			Binding binding = values[i];
			final Class clazz = binding.listener.getClass();
			
			if (binding.getTarget() == null) {
				bindings.remove(binding);
			}
			
			// A NamedEventListener?  Then check the eventName 
			else if (clazz == NamedEventListener.class) {
				NamedEventListener nel = (NamedEventListener)binding.listener;
				if (nel.isEmpty())
					bindings.remove(binding);
			}

			// If there is an array, it's an array of NamedEventListeners
			else if (clazz.isArray()){
				NamedEventListener[] nels = (NamedEventListener[])binding.listener;
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
					bindings.remove(binding);
				
			// By elimination, it must be a HashMap
			} else {
				assert(binding.listener.getClass() == HashMap.class);
				HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>)binding.listener;
				Object [] onels = map.values().toArray();
				for (int j = 0; j < onels.length; j++) {
					NamedEventListener nel = (NamedEventListener)onels[j];
					if (nel.isEmpty())
						map.remove(nel.eventName);
				}
				if (map.isEmpty())
					bindings.remove(binding);
			}
		}
		return bindings.isEmpty();
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
}
