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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.logging.log4j.Logger;

/**
 * Manages events and listeners on arbitrary objects.
 * 
 * The listeners are stored as Bindings in a LinkedList where the target is the
 * object to listen to and is a WeakReference. The listener is either: a) an
 * instance of NamedEventListener, b) a small array of NamedEventListeners, or
 * c) a HashMap where the key is the name of the event and the value is the
 * listener.
 * 
 * The NamedEventListener's listener object is either a small array of
 * EventListener's or a LinkedHashSet of EventListeners.
 * 
 * The reason for using Objects and reflection instead of always using HashMap
 * and LinkedHashSet is to keep the overhead down and support the common use
 * case where an object will have only a few events listened to, and each event
 * will only be listened to by a single piece of code (this is a guess).
 * 
 * Note that Maps cannot be used to track the bindings to a particular object
 * because that means using the target object as a key - and Maps require that
 * the hashCode for keys never changes, but List.hashCode always changes when
 * you change the contents of the list. Instead, LinkedList is used and every
 * time the object is found it is moved half way up the list so it's found
 * faster next time.
 * 
 * @author John Spackman [john.spackman@zenesis.com]
 */
public class EventManager {

  private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EventManager.class);

  /**
   * The size of arrays (as opposed to HashMaps) for lists of listeners on the
   * same object
   */
  private static final int TINY_ARRAY_SIZE = 5;

  /** How often to compact the bindings (in milliseconds) */
  private static final long COMPACT_FREQUENCY_MS = 5 * 60 * 1000;

  /*
   * Links an event name with a listener; the listener is actually either a) null,
   * b) an EventListener, c) an array of EventListeners, or d) a ArrayList of
   * EventListeners.
   */
  private static final class NamedEventListener {
    public final String eventName;
    public Object listenerRef;

    public NamedEventListener(String eventName, EventListener listener) {
      super();
      this.eventName = eventName;
      this.listenerRef = new WeakReference(listener);
    }

    /**
     * Adds a listener
     * 
     * @param newListener
     * @throws IllegalArgumentException if the listener is added twice
     */
    public void addListener(EventListener newListener) throws IllegalArgumentException {
      final Object listener = listenerRef != null && listenerRef instanceof Reference ? ((Reference)listenerRef).get() : listenerRef;

      // Nothing so far? Easy.
      if (listener == null) {
        listenerRef = new WeakReference(newListener);
        return;
      }

      final Class clazz = listener.getClass();

      // It's an array - try and use an empty slot
      if (clazz.isArray()) {
        WeakReference<EventListener>[] list = (WeakReference<EventListener>[]) listener;
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (list[i] != null && list[i].get() == newListener)
            throw new IllegalArgumentException("Cannot add the same listener to the same object and eventName twice");

        for (int i = 0; i < TINY_ARRAY_SIZE; i++) {
          // Free slot? Use it
          if (list[i] == null || list[i].get() == null) {
            list[i] = new WeakReference(newListener);
            return;
          }
        }

        // No room so upgrade to a ArrayList
        ArrayList<WeakReference<EventListener>> set = new ArrayList<WeakReference<EventListener>>();
        for (int i = 0; i < TINY_ARRAY_SIZE; i++) {
          if (list[i] != null && list[i].get() != null)
            set.add(list[i]);
        }
        set.add(new WeakReference(newListener));
        this.listenerRef = set;

        // Already a ArrayList - add to it
      } else if (clazz == ArrayList.class) {
        ArrayList<WeakReference<EventListener>> set = (ArrayList<WeakReference<EventListener>>) listener;
        int pos = -1;
        for (int i = 0; i < set.size(); i++) {
          WeakReference<EventListener> tmpRef = set.get(i);
          EventListener tmp = tmpRef != null ? tmpRef.get() : null;
          if (tmp == null && pos == -1)
            pos = i;
          else if (tmp == newListener)
            throw new IllegalArgumentException("Cannot add the same listener to the same object and eventName twice");
        }
        if (pos != -1)
          set.set(pos, new WeakReference(newListener));
        else
          set.add(new WeakReference(newListener));

        // Must be an EventListener instance, convert to an array
      } else {
        assert (EventListener.class.isAssignableFrom(clazz));
        if (listener == newListener)
          throw new IllegalArgumentException("Cannot add the same listener to the same object and eventName twice");
        WeakReference<EventListener>[] list = new WeakReference[TINY_ARRAY_SIZE];
        list[0] = (WeakReference<EventListener>) listenerRef;
        list[1] = new WeakReference(newListener);
        this.listenerRef = list;
      }
    }

    /**
     * Removes a listener
     * 
     * @param oldListener
     * @return true if the listener existed and was removed
     */
    public boolean removeListener(EventListener oldListener) {
      final Object listener = listenerRef != null && listenerRef instanceof Reference ? ((Reference)listenerRef).get() : listenerRef;
      if (listener == null)
        return false;

      final Class clazz = listener.getClass();

      // It's an array - find it and remove it
      if (clazz.isArray()) {
        WeakReference<EventListener>[] list = (WeakReference<EventListener>[]) listener;
        for (int i = 0; i < TINY_ARRAY_SIZE; i++) {
          if (list[i] != null && list[i].get() == oldListener) {
            list[i] = null;
            return true;
          }
        }
        return false;
      }

      // Already a ArrayList
      if (clazz == ArrayList.class) {
        ArrayList<WeakReference<EventListener>> set = (ArrayList<WeakReference<EventListener>>) listener;
        for (int i = 0; i < set.size(); i++) {
          WeakReference<EventListener> tmp = set.get(i);
          if (tmp != null && tmp.get() == oldListener) {
            set.set(i, null);
            return true;
          }
        }
        return false;
      }

      // Must be an EventListener instance
      assert (EventListener.class.isAssignableFrom(clazz));
      if (listener != oldListener)
        return false;

      listenerRef = null;
      return true;
    }

    /**
     * Removes a listener
     * 
     * @param oldListener
     * @return true if the listener existed and was removed
     */
    public boolean hasListener(EventListener oldListener) {
      final Object listener = listenerRef != null && listenerRef instanceof Reference ? ((Reference)listenerRef).get() : listenerRef;
      if (listener == null)
        return false;

      final Class clazz = listener.getClass();

      // It's an array - find it and remove it
      if (clazz.isArray()) {
        WeakReference<EventListener>[] list = (WeakReference<EventListener>[]) listener;
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (list[i] != null && list[i].get() == oldListener)
            return true;
        return false;
      }

      // Already a ArrayList
      if (clazz == ArrayList.class) {
        ArrayList<WeakReference<EventListener>> set = (ArrayList<WeakReference<EventListener>>) listener;
        for (WeakReference<EventListener> tmp : set)
          if (tmp != null && tmp.get() == oldListener)
            return true;
        return false;
      }

      // Must be an EventListener instance
      assert (clazz == EventListener.class);
      if (listener != oldListener)
        return false;
      return true;
    }

    /**
     * Fires an event on the listener(s)
     */
    public void fireEvent(Event event) {
      final Object listener = listenerRef != null && listenerRef instanceof Reference ? ((Reference)listenerRef).get() : listenerRef;

      if (listener == null)
        return;

      final Class clazz = listener.getClass();

      // It's an array - find it and remove it
      if (clazz.isArray()) {
        WeakReference<EventListener>[] list = (WeakReference<EventListener>[]) listener;
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (list[i] != null) {
            EventListener entry = list[i].get();
            if (entry != null)
              entry.handleEvent(event);
          }
        return;
      }

      // Already a ArrayList
      if (clazz == ArrayList.class) {
        ArrayList<WeakReference<EventListener>> set = (ArrayList<WeakReference<EventListener>>) listener;
        for (WeakReference<EventListener> ref : set) {
          if (ref != null) {
            EventListener entry = ref.get();
            if (entry != null)
              entry.handleEvent(event);
          }
        }
        return;
      }

      // Must be an EventListener instance, convert to an array
      assert (EventListener.class.isAssignableFrom(clazz));
      EventListener entry = (EventListener) listener;
      entry.handleEvent(event);
    }

    public boolean isEmpty() {
      final Object listener = listenerRef != null && listenerRef instanceof Reference ? ((Reference)listenerRef).get() : listenerRef;

      if (listener == null)
        return true;

      final Class clazz = listener.getClass();

      // It's an array
      if (clazz.isArray()) {
        WeakReference<EventListener>[] list = (WeakReference<EventListener>[]) listener;
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (list[i] != null && list[i].get() != null)
            return false;
        return true;
      }

      // Already a ArrayList
      if (clazz == ArrayList.class) {
        ArrayList<WeakReference<EventListener>> set = (ArrayList<WeakReference<EventListener>>) listener;
        for (WeakReference<EventListener> ref : set)
          if (ref != null && ref.get() != null)
            return false;
        return true;
      }

      // Must be an EventListener instance
      assert (EventListener.class.isAssignableFrom(clazz));
      return listener == null;
    }
  }

  public static class BoundListeners {
    public Object listener;

    /**
     * Adds an event listener
     * 
     * @param eventName
     * @param listener
     * @throws {@link IllegalArgumentException} if a listener is added twice
     * @return true if the event was added
     */
    public synchronized boolean addListener(String eventName, EventListener newListener) {
      // If the object is not yet known, then create a new NEL and return
      if (listener == null) {
        NamedEventListener nel = new NamedEventListener(eventName, newListener);
        listener = nel;
        return true;
      }

      final Class clazz = listener.getClass();

      // A NamedEventListener? Then we've found it
      if (clazz == NamedEventListener.class) {
        NamedEventListener nel = (NamedEventListener) listener;
        if (nel.eventName.equals(eventName))
          nel.addListener(newListener);
        else {
          NamedEventListener[] nels = new NamedEventListener[TINY_ARRAY_SIZE];
          nels[0] = nel;
          nels[1] = new NamedEventListener(eventName, newListener);
          listener = nels;
        }
        return true;
      }

      // If there is an array, it's an array of NamedEventListeners
      if (clazz.isArray()) {
        NamedEventListener[] nels = (NamedEventListener[]) listener;

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
          nels[index].addListener(newListener);

          // Found a free slot?
        } else if (freeIndex > -1) {
          NamedEventListener nel = nels[freeIndex] = new NamedEventListener(eventName, null);
          nel.addListener(newListener);

          // The array is full - convert to a HashMap
        } else {
          // Convert to a map
          HashMap<String, NamedEventListener> map = new HashMap<String, NamedEventListener>();
          for (int i = 0; i < TINY_ARRAY_SIZE; i++)
            map.put(nels[i].eventName, nels[i]);

          // Add the new NamedEventListener
          map.put(eventName, new NamedEventListener(eventName, newListener));

          // Replace the array with the map
          listener = map;
        }

        return true;
      }

      // By elimination, it must be a HashMap
      assert (listener.getClass() == HashMap.class);
      HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>) listener;
      NamedEventListener nel = map.get(eventName);
      if (nel == null)
        map.put(eventName, new NamedEventListener(eventName, newListener));
      else
        nel.addListener(newListener);

      return true;
    }

    /**
     * Removes a listener
     * 
     * @param eventName   name of the event to remove
     * @param oldListener if null, all listeners are removed
     * @return true if a listener was removed
     */
    public synchronized boolean removeListener(String eventName, EventListener oldListener) {
      if (listener == null)
        return false;

      final Class clazz = listener.getClass();

      // A NamedEventListener? Then check the eventName
      if (clazz == NamedEventListener.class) {
        NamedEventListener nel = (NamedEventListener) listener;
        if (eventName != null && !nel.eventName.equals(eventName))
          return false;
        if (oldListener == null) {
          listener = null;
          return true;
        }
        return nel.removeListener(oldListener);
      }

      // If there is an array, it's an array of NamedEventListeners
      if (clazz.isArray()) {
        NamedEventListener[] nels = (NamedEventListener[]) listener;
        boolean removed = false;

        // Look for a NamedEventListener for the eventName
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (nels[i] != null)
            if (eventName == null || nels[i].eventName.equals(eventName)) {
              if (oldListener == null) {
                nels[i] = null;
                removed = true;
              } else if (nels[i].removeListener(oldListener))
                removed = true;
            }

        return removed;
      }

      // By elimination, it must be a HashMap
      assert (listener.getClass() == HashMap.class);
      HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>) listener;
      if (eventName == null) {
        boolean removed = false;
        for (NamedEventListener nel : map.values())
          if (nel.removeListener(oldListener))
            removed = true;
        return removed;
      }
      if (oldListener == null)
        return map.remove(eventName) != null;

      NamedEventListener nel = map.get(eventName);
      if (nel == null)
        return false;
      return nel.removeListener(oldListener);
    }

    /**
     * Detects whether an event is being listened for
     * 
     * @param eventName   name of the event
     * @param newListener if not null, detects whether this specific listener
     *                    instance is in use
     * @return true if found
     */
    public synchronized boolean hasListener(String eventName, EventListener newListener) {
      if (listener == null)
        return false;

      final Class clazz = listener.getClass();

      // A NamedEventListener? Then check the eventName
      if (clazz == NamedEventListener.class) {
        NamedEventListener nel = (NamedEventListener) listener;
        if (!nel.eventName.equals(eventName))
          return false;
        if (newListener == null)
          return true;
        return nel.hasListener(newListener);
      }

      // If there is an array, it's an array of NamedEventListeners
      if (clazz.isArray()) {
        NamedEventListener[] nels = (NamedEventListener[]) listener;

        // Look for a NamedEventListener for the eventName
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (nels[i].eventName.equals(eventName)) {
            if (newListener == null)
              return true;
            return nels[i].hasListener(newListener);
          }

        return false;
      }

      // By elimination, it must be a HashMap
      assert (listener.getClass() == HashMap.class);
      HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>) listener;
      NamedEventListener nel = map.get(eventName);
      if (nel == null)
        return false;
      if (newListener == null)
        return true;
      return nel.hasListener(newListener);
    }

    /**
     * Detects whether there are any listeners
     * 
     * @return true if there are listeners
     */
    public synchronized boolean isEmpty() {
      if (listener == null)
        return true;

      final Class clazz = listener.getClass();

      // A NamedEventListener? Then check the eventName
      if (clazz == NamedEventListener.class)
        return ((NamedEventListener) listener).isEmpty();

      // If there is an array, it's an array of NamedEventListeners
      if (clazz.isArray()) {
        NamedEventListener[] nels = (NamedEventListener[]) listener;

        // Look for a NamedEventListener for the eventName
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (nels[i] != null && !nels[i].isEmpty())
            return false;

        return true;
      }

      // By elimination, it must be a HashMap
      assert (listener.getClass() == HashMap.class);
      HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>) listener;
      if (map.isEmpty())
        return true;
      for (NamedEventListener nel : map.values())
        if (!nel.isEmpty())
          return false;
      return true;
    }

    /**
     * Fires an event on all listeners
     * 
     * @param event
     */
    public void fireDataEvent(Event event) {
      if (listener == null)
        return;

      final Class clazz = listener.getClass();

      // A NamedEventListener? Then check the eventName
      if (clazz == NamedEventListener.class) {
        NamedEventListener nel = (NamedEventListener) listener;
        if (!nel.eventName.equals(event.getEventName()))
          return;
        nel.fireEvent(event);
        return;
      }

      // If there is an array, it's an array of NamedEventListeners
      if (clazz.isArray()) {
        NamedEventListener[] nels = (NamedEventListener[]) listener;

        // Look for a NamedEventListener for the eventName
        for (int i = 0; i < TINY_ARRAY_SIZE; i++)
          if (nels[i] != null && nels[i].eventName.equals(event.getEventName()))
            nels[i].fireEvent(event);

        return;

      }

      // By elimination, it must be a HashMap
      assert (listener.getClass() == HashMap.class);
      HashMap<String, NamedEventListener> map = (HashMap<String, NamedEventListener>) listener;
      NamedEventListener nel = map.get(event.getEventName());
      if (nel == null)
        return;
      nel.fireEvent(event);
    }

  }

  /*
   * Records the binding between a target object and the listeners; listener is
   * either a NamedEventListener, an array of NamedEventListeners, or a Map
   * indexed by name
   */
  private static final class Binding extends BoundListeners {
    public WeakReference targetRef;

    public Binding(Object target) {
      super();
      this.targetRef = new WeakReference(target);
    }

    public Object getTarget() {
      return targetRef.get();
    }
  }

  /*
   * Can be passed with changeXxxx events
   */
  public static class ChangeValue {
    private final Object value;
    private final Object oldValue;

    public ChangeValue(Object value, Object oldValue) {
      super();
      this.value = value;
      this.oldValue = oldValue;
    }

    public Object getValue() {
      return value;
    }

    public Object getOldValue() {
      return oldValue;
    }
  }

  // Linked list of bindings - note this cannot be a map because maps require the
  // target to be immutable (Collections change their hashCode as they are
  // modified)
  private LinkedList<Binding> bindings = new LinkedList<Binding>();

  // When the bindings were last compacted
  private long lastCompacted;

  private static EventManager s_instance;

  /**
   * Constructor; the first instance of EventManager will become the global
   * default
   */
  public EventManager() {
    this(true);
  }

  /**
   * Constructor; the first instance of EventManager will become the global
   * default only if setGlobal is true
   * 
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
   * 
   * @param keyObject
   * @param eventName
   * @param listener
   * @throws {@link IllegalArgumentException} if a listener is added twice
   * @return true if the event was added
   */
  public static boolean addListener(Object keyObject, String eventName, EventListener listener)
      throws IllegalArgumentException {
    if (keyObject instanceof Eventable)
      return ((Eventable) keyObject).addListener(eventName, listener);
    return getInstance()._addListener(keyObject, eventName, listener);
  }

  /**
   * Adds an event listener
   * 
   * @param keyObject
   * @param eventName
   * @param listener
   * @throws {@link IllegalArgumentException} if a listener is added twice
   * @return true if the event was added
   */
  protected synchronized boolean _addListener(Object keyObject, String eventName, EventListener listener)
      throws IllegalArgumentException {
    if (!supportsEvent(keyObject, eventName))
      return false;

    Binding binding = getBinding(keyObject);

    // If the object is not yet known, then create a new NEL and return
    if (binding == null) {
      binding = new Binding(keyObject);
      binding.addListener(eventName, listener);
      bindings.push(binding);
      return true;
    }

    return binding.addListener(eventName, listener);
  }

  /**
   * Removes a listener from an object and eventName
   * 
   * @param keyObject
   * @param eventName
   * @param listener
   * @return
   */
  public static boolean removeListener(Object keyObject, String eventName, EventListener listener) {
    if (keyObject instanceof Eventable)
      return ((Eventable) keyObject).removeListener(eventName, listener);
    return getInstance()._removeListener(keyObject, eventName, listener);
  }

  /**
   * Removes a listener from an object and eventName
   * 
   * @param keyObject
   * @param eventName
   * @param listener
   * @return
   */
  protected synchronized boolean _removeListener(Object keyObject, String eventName, EventListener listener) {
    if (eventName == null && listener == null)
      return removeBinding(keyObject);

    Binding binding = getBinding(keyObject);
    if (binding == null)
      return false;

    boolean removed = binding.removeListener(eventName, listener);
    if (binding.isEmpty()) {
      bindings.remove(binding);
    }
    return removed;
  }

  /**
   * Removes a listener from an object and eventName
   * 
   * @param keyObject
   * @param eventName
   * @param listener
   * @return
   */
  public static boolean hasListener(Object keyObject, String eventName, EventListener listener) {
    if (keyObject instanceof Eventable)
      return ((Eventable) keyObject).hasListener(eventName, listener);
    return getInstance()._hasListener(keyObject, eventName, listener);
  }

  /**
   * Removes a listener from an object and eventName
   * 
   * @param keyObject
   * @param eventName
   * @param listener
   * @return
   */
  protected synchronized boolean _hasListener(Object keyObject, String eventName, EventListener listener) {
    Binding binding = getBinding(keyObject);
    if (binding == null)
      return false;

    return binding.hasListener(eventName, listener);
  }

  /**
   * Detects whether the object supports the given event name; by default, all
   * objects are considered to support events.
   * 
   * @param obj
   * @param eventName
   * @return
   */
  public static boolean supportsEvent(Object obj, String eventName) {
    return getInstance()._supportsEvent(obj, eventName);
  }

  /**
   * Detects whether the object supports the given event name; by default, all
   * objects are considered to support events.
   * 
   * @param obj
   * @param eventName
   * @return
   */
  protected boolean _supportsEvent(Object obj, String eventName) {
    if (obj instanceof EventVerifiable) {
      EventVerifiable ev = (EventVerifiable) obj;
      return ev.supportsEvent(eventName);
    }

    return true;
  }

  /**
   * Fires an event on the object
   * 
   * @param obj
   * @param eventName
   */
  public static void fireEvent(Object keyObject, String eventName) {
    if (keyObject instanceof Eventable) {
      ((Eventable) keyObject).fireDataEvent(eventName, null);
      return;
    }
    getInstance().fireDataEvent(new Event(keyObject, keyObject, eventName, null));
  }

  /**
   * Fires a data event on the object
   * 
   * @param obj
   * @param eventName
   * @param data
   */
  public static void fireDataEvent(Object keyObject, String eventName, Object data) {
    if (keyObject instanceof Eventable) {
      ((Eventable) keyObject).fireDataEvent(eventName, data);
      return;
    }
    getInstance().fireDataEvent(new Event(keyObject, keyObject, eventName, data));
  }

  /**
   * Fires a data event on the object
   * 
   * @param obj
   * @param eventName
   * @param data
   */
  public void fireDataEvent(Event event) {
    Binding binding = getBinding(event.getOriginalTarget());
    if (binding == null)
      return;

    binding.fireDataEvent(event);
  }

  /**
   * Finds the binding for an object, and moves it forward in the list if found
   * 
   * @param target
   * @return
   */
  private synchronized Binding getBinding(Object target) {
    if (System.currentTimeMillis() - COMPACT_FREQUENCY_MS > lastCompacted)
      compact();

    for (int index = 0; index < bindings.size(); index++) {
      Binding bind = bindings.get(index);
      if (bind.getTarget() == target) {
        if (index > 1) {
          bindings.remove(index);
          bindings.add(index / 2, bind);
        }
        return bind;
      }
    }
    return null;
  }

  /**
   * Removes the binding for the target
   * 
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
   * Compacts the lists, removing everything that has empty references and returns
   * whether there are any event listeners on any object
   * 
   * @return
   */
  public synchronized boolean compact() {
    Binding[] values = bindings.toArray(new Binding[bindings.size()]);
    for (int i = 0; i < values.length; i++) {
      Binding binding = values[i];

      if (binding.getTarget() == null || binding.isEmpty()) {
        bindings.remove(binding);
      }
    }
    lastCompacted = System.currentTimeMillis();
    return bindings.isEmpty();
  }

  public synchronized int size() {
    return bindings.size();
  }

  /**
   * Returns the default global instance
   * 
   * @return
   */
  public static EventManager getInstance() {
    if (s_instance == null)
      new EventManager(true);
    return s_instance;
  }
}
