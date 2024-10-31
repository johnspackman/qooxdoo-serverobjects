package com.zenesis.qx.remote.collections;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.zenesis.core.HasUuid;
import com.zenesis.qx.event.EventListener;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.event.EventStore;
import com.zenesis.qx.event.EventVerifiable;
import com.zenesis.qx.event.Eventable;
import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Mixin;
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.SerializeConstructorArgs;

/**
 * Provides an implementation of ArrayList which monitors changes to the array
 * and queues them for serialisation to the client via ProxyManager
 *
 * @author John Spackman
 */
@Properties(extend = "qx.data.Array")
@Mixin(value = "com.zenesis.qx.remote.MArrayList")
public class ArrayList<T> extends java.util.AbstractList<T> implements Proxied, Eventable, EventVerifiable {

  private static Logger log = LogManager.getLogger(ArrayList.class);

  private final EventStore eventStore = new EventStore(this);
  private final int hashCode;
  private boolean sorting;
  private transient Object[] elementData;
  private int size;
  private boolean storeReferences;
  private Object containerObject;
  @Property
  private boolean detectDuplicates;
  @Property
  private boolean detectNulls;

  public ArrayList() {
    this(5);
  }

  public ArrayList(Collection<? extends T> c) {
    hashCode = new Object().hashCode();
    size = c.size();
    elementData = new Object[size];
    Iterator it = c.iterator();
    int i = 0;
    while (it.hasNext()) {
      Object value = it.next();
      if (detectNulls && value == null) {
        new IllegalArgumentException("Adding null value to protected array");
      }
      elementData[i++] = value;
    }
  }

  public ArrayList(int initialCapacity) {
    hashCode = new Object().hashCode();
    elementData = new Object[initialCapacity];
  }

  public boolean isDetectNulls() {
    return detectNulls;
  }

  public void setDetectNulls(boolean detectNulls) {
    this.detectNulls = detectNulls;
  }

  public boolean isDetectDuplicates() {
    return detectDuplicates;
  }

  public void setDetectDuplicates(boolean detectDuplicates) {
    this.detectDuplicates = detectDuplicates;
  }

  /**
   * Scans the array looking for duplicates;
   *
   * @return
   */
  public boolean detectDuplicates() {
    for (int i = 0; i < size; i++) {
      Object check = elementData[i];
      OnDemandReference checkRef = (check instanceof OnDemandReference) ? (OnDemandReference) check : null;
      String checkUuid = checkRef != null ? checkRef.getUuid()
          : check instanceof HasUuid ? ((HasUuid) check).getUuid() : null;
      for (int j = i + 1; j < size; j++) {
        Object element = elementData[j];
        OnDemandReference elementRef = (element instanceof OnDemandReference) ? (OnDemandReference) element : null;
        String elementUuid = elementRef != null ? elementRef.getUuid()
            : element instanceof HasUuid ? ((HasUuid) element).getUuid() : null;

        if (checkUuid != null && elementUuid != null) {
          if (checkUuid.equalsIgnoreCase(elementUuid)) {
            System.out.println("Duplicate detected!");
            return true;
          }
        }
        if (element == check) {
          System.out.println("Duplicate detected!");
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Scans the array looking for nulls; raises an exception if it finds one and
   * `detectNulls` is true
   *
   * @return
   */
  public boolean detectNulls() {
    for (int i = 0; i < size; i++) {
      Object element = elementData[i];
      if (element == null) {
        if (detectNulls)
          throw new IllegalStateException("Detected null in protected array");
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean contains(Object obj) {
    detectNulls();
    String uuid;
    T target = null;
    OnDemandReference<T> ref = null;
    if (obj instanceof OnDemandReference<?>) {
      ref = (OnDemandReference) obj;
      uuid = ref.getUuid();
    } else if (obj instanceof HasUuid) {
      uuid = ((HasUuid) obj).getUuid();
      target = (T) obj;
    } else
      return super.contains(obj);

    for (int i = 0; i < size; i++) {
      Object element = elementData[i];
      if (element instanceof OnDemandReference) {
        String refUuid = ((OnDemandReference) element).getUuid();
        if (refUuid.equals(uuid))
          return true;
      } else {
        if (target == null && ref != null) {
          target = ref.get();
          if (detectNulls && target == null) {
            log.warn("OnDemandReference returned null for uuid=" + ref.getUuid());
          }
        }
        if (element == target || (element != null && element.equals(target)))
          return true;
      }
    }

    return false;
  }

  /**
   * Gets the UUIDs of each item in the ArrayList
   *
   * @return ArrayList containing all UUIDs
   */
  public ArrayList<String> getUuids() {
    detectNulls();
    ArrayList<String> uuids = new ArrayList<String>();
    for (T obj : this) {
      if (obj instanceof OnDemandReference<?>) {
        uuids.add(((OnDemandReference) obj).getUuid());
      } else if (obj instanceof HasUuid) {
        uuids.add(((HasUuid) obj).getUuid());
      }
    }
    return uuids;
  }

  @Override
  public int indexOf(Object obj) {
    detectNulls();
    String uuid;
    T target = null;
    OnDemandReference<T> ref = null;
    if (obj instanceof OnDemandReference<?>) {
      ref = (OnDemandReference) obj;
      uuid = ref.getUuid();
    } else if (obj instanceof HasUuid) {
      uuid = ((HasUuid) obj).getUuid();
      target = (T) obj;
    } else
      return super.indexOf(obj);

    for (int i = 0; i < size; i++) {
      Object element = elementData[i];

      if (element instanceof OnDemandReference) {
        String refUuid = ((OnDemandReference) element).getUuid();
        if (refUuid.equals(uuid))
          return i;
      } else {
        if (target == null && ref != null) {
          target = ref.get();
          if (detectNulls && target == null) {
            log.warn("OnDemandReference returned null for uuid=" + ref.getUuid());
          }
        }
        if (element == target || element.equals(target))
          return i;
      }
    }

    return -1;
  }

  @Override
  public T get(int index) {
    detectNulls();
    if (index < 0 || index > size)
      throw new IndexOutOfBoundsException("Index " + size + " out of bounds, max=" + size);
    Object obj = elementData[index];
    if (obj == null)
      return null;
    if (obj instanceof OnDemandReference<?>) {
      obj = ((OnDemandReference<T>) obj).get();
    }
    return (T) obj;
  }

  /**
   * Finds an object by UUID; only loads on demand objects if they match the UUID
   *
   * @param uuid
   * @return
   */
  public T findByUuid(String uuid) {
    detectNulls();
    for (int i = 0; i < size; i++) {
      Object obj = elementData[i];
      if (obj instanceof OnDemandReference<?>) {
        OnDemandReference<T> odr = (OnDemandReference<T>) obj;
        if (odr.getUuid().equals(uuid)) {
          return odr.get();
        }
      } else if (obj instanceof HasUuid) {
        if (((HasUuid) obj).getUuid().equals(uuid)) {
          return (T) obj;
        }
      }
    }
    return null;
  }

  /**
   * Tests whether object with a UUID exists; never loads on demand objects
   *
   * @param uuid
   * @return
   */
  public boolean containsUuid(String uuid) {
    detectNulls();
    for (int i = 0; i < size; i++) {
      Object obj = elementData[i];
      if (obj instanceof OnDemandReference<?>) {
        OnDemandReference<T> odr = (OnDemandReference<T>) obj;
        if (odr.getUuid().equals(uuid)) {
          return true;
        }
      } else if (obj instanceof HasUuid) {
        if (((HasUuid) obj).getUuid().equals(uuid)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets the object in the array, but just the actual raw data - this would be an
   * OnDemandReference and not the object which has been refered to
   *
   * @param index
   * @return
   */
  public Object getRawElement(int index) {
    if (index < 0 || index > size)
      throw new IndexOutOfBoundsException("Index " + size + " out of bounds, max=" + size);
    return elementData[index];
  }

  @Override
  public int size() {
    return size;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ArrayList#add(int, java.lang.Object)
   */
  @Override
  public void add(int index, T element) {
    addImpl(index, element);
  }

  public void add(OnDemandReference<T> ref) {
    addImpl(size, ref);
  }

  private void addImpl(int index, Object element) {
    if (detectDuplicates) {
      String uuid = null;
      if (element instanceof OnDemandReference)
        uuid = ((OnDemandReference) element).getUuid();
      else if (element instanceof HasUuid)
        uuid = ((HasUuid) element).getUuid();
      for (int i = 0; i < size; i++) {
        Object obj = elementData[i];
        if (obj instanceof OnDemandReference<?>) {
          OnDemandReference<T> odr = (OnDemandReference<T>) obj;
          if (odr.getUuid().equals(uuid)) {
            System.out.println("Adding a duplicate");
          }
        } else if (obj instanceof HasUuid) {
          if (((HasUuid) obj).getUuid().equals(uuid)) {
            System.out.println("Adding a duplicate");
          }
        }
      }
    }
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before adding");
    }
    detectNulls();
    if (detectNulls && element == null) {
      throw new IllegalArgumentException("Adding null not allowed, index=" + index);
    }
    if (index < -1 || index > size)
      throw new IndexOutOfBoundsException("Cannot add with index=" + index + " when size=" + size);
    if (size == this.elementData.length) {
      Object[] tmp = new Object[size + 5];
      System.arraycopy(elementData, 0, tmp, 0, elementData.length);
      elementData = tmp;
    }
    System.arraycopy(elementData, index,
        elementData, index + 1,
        size - index);
    elementData[index] = getValueToStore(null, element);
    size++;
    fire(new ArrayChangeData().add(element));
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after adding");
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ArrayList#remove(int)
   */
  @Override
  public T remove(int index) {
    if (index < 0 || index >= size)
      throw new IndexOutOfBoundsException("Cannot remove with index=" + index + " when size=" + size);
    detectNulls();
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before removing");
    }
    T result = get(index);
    if (index < size - 1) {
      System.arraycopy(elementData, index + 1, elementData, index, size - index - 1);
    }
    elementData[size - 1] = null;
    size--;

    fire(new ArrayChangeData().remove(result));
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after removing");
    }
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ArrayList#set(int, java.lang.Object)
   */
  @Override
  public T set(int index, T element) {
    T result = get(index);

    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before setting");
    }
    detectNulls();
    if (detectNulls && element == null) {
      throw new IllegalArgumentException("Setting null not allowed, index=" + index);
    }
    elementData[index] = getValueToStore(elementData[index], element);

    fire(new ArrayChangeData().remove(result).add(element));
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after setting");
    }
    return result;
  }

  /**
   * Figures out the best value to actually store in the array, converting to an
   * OnDemandReference
   * where possible; it will also reuse existing OnDemandReference in preference
   * to creating a new
   * one
   * 
   * This method is called when we add or replace an element in the array
   * and we want to check the best value to actually store in the array.
   * 
   * @param oldValue If we are replacing an element, this is the old, raw value stored in the array
   * @param element The new value to store in the array
   * @return
   */
  private Object getValueToStore(Object oldValue, Object element) {
    if (element == null)
      return null;
    if (element instanceof OnDemandReference<?>)
      return element;
    if (oldValue instanceof OnDemandReference<?>) {
      ((OnDemandReference<T>) oldValue).set((T) element);
      return oldValue;
    }
    if (isStoreReferences()) {
      OnDemandReference ref = OnDemandReferenceFactory.createReferenceFor(element.getClass(), containerObject);
      if (ref != null) {
        ref.set(element);
        return ref;
      }
    }
    return element;
  }

  public boolean isStoreReferences() {
    return storeReferences;
  }

  public void setStoreReferences(boolean storeReferences) {
    this.storeReferences = storeReferences;
  }

  @Override
  public boolean supportsEvent(String eventName) {
    return eventName.equals("change");
  }

  @Override
  public void disableEvents() {
    eventStore.disableEvents();
  }

  @Override
  public void enableEvents() {
    eventStore.enableEvents();
  }

  @Override
  public boolean eventsEnabled() {
    return eventStore.eventsEnabled();
  }

  @Override
  public boolean addListener(String eventName, EventListener listener) throws IllegalArgumentException {
    return eventStore.addListener(eventName, listener);
  }

  @Override
  public boolean removeListener(String eventName, EventListener listener) {
    return eventStore.removeListener(eventName, listener);
  }

  @Override
  public boolean hasListener(String eventName, EventListener listener) {
    return eventStore.hasListener(eventName, listener);
  }

  @Override
  public void fireEvent(String eventName) {
    eventStore.fireEvent(eventName);
  }

  @Override
  public void fireDataEvent(String eventName, Object data) {
    eventStore.fireDataEvent(eventName, data);
  }

  public Object getContainerObject() {
    return containerObject;
  }

  public void setContainerObject(Object containerObject) {
    this.containerObject = containerObject;
  }

  /**
   * Hook to specify serialisation of teh constructor
   *
   * @param jgen
   * @throws IOException
   */
  @SerializeConstructorArgs
  public void serializeConstructorArgs(JsonGenerator jgen) throws IOException {
    jgen.writeStartArray();
    Object[] arr;
    while (true) {
      try {
        arr = toArray();
        break;
      } catch (ConcurrentModificationException e) {
        // Nothing
      }
    }
    for (Object value : arr)
      jgen.writeObject(value);
    jgen.writeEndArray();
  }

  /**
   * Sorts the list
   */
  public void sort() {
    sort(null);
  }

  /**
   * Replaces the contents of the array with another
   *
   * @param collection
   */
  public void replace(Collection<? extends T> collection) {
    clear();
    for (T element : collection) {
      if (detectNulls && element == null) {
        throw new IllegalArgumentException("Adding null not allowed");
      }
      add(element);
    }
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after replace");
    }
    detectNulls();
  }

  /**
   * Sorts the list with a comparator
   *
   * @param comp
   */
  @Override
  public void sort(Comparator<? super T> comp) {
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before sort");
    }
    sorting = true;
    try {
      super.sort(comp);
    } finally {
      sorting = false;
    }
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after sort");
    }
    detectNulls();
  }

  @Override
  public Iterator<T> iterator() {
    return new ValueIterator();
  }

  Iterator<T> superIterator() {
    return super.iterator();
  }

  /**
   * @return true if the list is being sorted
   */
  public boolean isSorting() {
    return sorting;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ArrayList#addAll(java.util.Collection)
   */
  @Override
  public boolean addAll(Collection c) {
    boolean result = super.addAll(c);
    if (!result)
      return false;
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before addAll");
    }
    detectNulls();
    ArrayChangeData event = new ArrayChangeData();
    for (Object element : c) {
      if (detectNulls && element == null) {
        throw new IllegalArgumentException("Adding null not allowed");
      }
      event.add(element);
    }
    fire(event);
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after addAll");
    }
    detectNulls();
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ArrayList#addAll(int, java.util.Collection)
   */
  @Override
  public boolean addAll(int index, Collection c) {
    boolean result = super.addAll(index, c);
    if (!result)
      return false;
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before addAll(i,c)");
    }
    ArrayChangeData event = new ArrayChangeData();
    for (Object element : c) {
      if (detectNulls && element == null) {
        throw new IllegalArgumentException("Adding null not allowed, index=" + index);
      }
      event.add(element);
    }
    fire(event);
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after addAll(i,c)");
    }
    detectNulls();
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ArrayList#clear()
   */
  @Override
  public void clear() {
    if (size() == 0)
      return;
    ArrayChangeData event = new ArrayChangeData();
    for (Object o : this)
      event.remove(o);
    super.clear();
    fire(event);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.ArrayList#removeRange(int, int)
   */
  @Override
  public void removeRange(int fromIndex, int toIndex) {
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before removeRange");
    }
    if (toIndex < fromIndex || fromIndex < 0)
      throw new IllegalArgumentException();
    if (toIndex == fromIndex)
      return;

    ArrayChangeData event = new ArrayChangeData();
    for (int i = fromIndex; i < toIndex; i++)
      event.remove(this.get(i));
    super.removeRange(fromIndex, toIndex);
    fire(event);
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after removeRange");
    }
    detectNulls();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.AbstractCollection#removeAll(java.util.Collection)
   */
  @Override
  public boolean removeAll(Collection c) {
    if (isEmpty() || c.isEmpty())
      return false;
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before removeAll");
    }
    ArrayChangeData event = new ArrayChangeData<T>();
    for (Object o : c)
      if (contains(o))
        event.remove(o);
    boolean result = super.removeAll(c);
    fire(event);
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after removeAll");
    }
    detectNulls();
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.util.AbstractCollection#retainAll(java.util.Collection)
   */
  @Override
  public boolean retainAll(Collection c) {
    if (isEmpty())
      return false;
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates before retainAll");
    }
    ArrayChangeData event = new ArrayChangeData<T>();
    for (Object o : this)
      if (!c.contains(o))
        event.remove(o);
    boolean result = super.retainAll(c);
    fire(event);
    if (detectDuplicates && detectDuplicates()) {
      System.out.println("detected duplicates after retainAll");
    }
    detectNulls();
    return result;
  }

  @Override
  public int hashCode() {
    /*
     * We MUST override hashcode because ProxyManager depends on HashMap to lookup
     * the server ID for an object, but in Java the hashCode changes as you modify
     * the collection; this means that mutable collections cannot be used as keys in
     * maps. The hashcode is generated during construction so that there is a kind
     * of distribution of values (rather than just returning 1 here)
     */
    return hashCode;
  }

  /**
   * Fires an event
   *
   * @param event
   */
  private void fire(ArrayChangeData event) {
    if (eventsEnabled())
      EventManager.fireDataEvent(this, "change", event);
    ProxyManager.collectionChanged(this, event);
  }

  /**
   * Class for change events
   *
   * @param <T>
   */
  public static class ArrayChangeData<T> extends ChangeData {
    public java.util.ArrayList<T> added;
    public java.util.ArrayList<T> removed;

    public ArrayChangeData add(T o) {
      if (removed == null || !removed.remove(o)) {
        if (added == null)
          added = new java.util.ArrayList<T>(5);
        added.add(o);
      }
      return this;
    }

    public ArrayChangeData remove(T o) {
      if (added == null || !added.remove(o)) {
        if (removed == null)
          removed = new java.util.ArrayList<T>(5);
        removed.add(o);
      }
      return this;
    }

  }

  /**
   * Iterator class
   */
  private final class ValueIterator implements Iterator<T> {

    private final Iterator<T> iterator;
    private T last;

    public ValueIterator() {
      iterator = superIterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public T next() {
      return last = iterator.next();
    }

    @Override
    public void remove() {
      fire(new ArrayChangeData().remove(last));
      iterator.remove();
    }

  }

}