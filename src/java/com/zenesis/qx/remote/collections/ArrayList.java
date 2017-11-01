package com.zenesis.qx.remote.collections;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.SerializeConstructorArgs;
import com.zenesis.qx.remote.annotations.Properties;

/**
 * Provides an implementation of ArrayList which monitors changes to the array
 * and queues them for serialisation to the client via ProxyManager 
 * 
 * @author John Spackman
 */
@SuppressWarnings("serial")
@Properties(extend="qx.data.Array")
public class ArrayList<T> extends java.util.ArrayList<T> implements Proxied {
	
	private final int hashCode;
	private boolean sorting;
	
	public ArrayList() {
		super();
		hashCode = new Object().hashCode();
	}

	public ArrayList(Collection<? extends T> c) {
		super(c);
		hashCode = new Object().hashCode();
	}

	public ArrayList(int initialCapacity) {
		super(initialCapacity);
		hashCode = new Object().hashCode();
	}

	@SerializeConstructorArgs
	public void serializeConstructorArgs(JsonGenerator jgen) throws IOException {
		jgen.writeStartArray();
		Object[] arr;
		while (true) {
			try {
				arr = toArray();
				break;
			} catch(ConcurrentModificationException e) {
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
	
	public void replace(Collection<? extends T> c) {
	    clear();
	    for (T x : c)
	        add(x);
	}
	
	/**
	 * Sorts the list with a comparator
	 * @param comp
	 */
	@Override 
    public void sort(Comparator<? super T> comp) {
		sorting = true;
		try {
			super.sort(comp);
		} finally {
			sorting = false;
		}
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

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, T element) {
		super.add(index, element);
		fire(new ArrayChangeData().add(element));
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(T e) {
		boolean result = super.add(e);
		fire(new ArrayChangeData().add(e));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection c) {
		boolean result = super.addAll(c);
		if (!result)
			return false;
		ArrayChangeData event = new ArrayChangeData();
		for (Object o : c)
			event.add(o);
		fire(event);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(int, java.util.Collection)
	 */
	@Override
	public boolean addAll(int index, Collection c) {
		boolean result = super.addAll(index, c);
		if (!result)
			return false;
		ArrayChangeData event = new ArrayChangeData();
		for (Object o : c)
			event.add(o);
		fire(event);
		return result;
	}

	/* (non-Javadoc)
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

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(int)
	 */
	@Override
	public T remove(int index) {
		T result = super.remove(index);
		fire(new ArrayChangeData().remove(result));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		boolean result = super.remove(o);
		if (result)
			fire(new ArrayChangeData().remove(o));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#removeRange(int, int)
	 */
	@Override
	public void removeRange(int fromIndex, int toIndex) {
		if (toIndex >= size())
			toIndex = size() - 1;
		if (toIndex < fromIndex || fromIndex < 0)
			throw new IllegalArgumentException();
		if (toIndex == fromIndex)
			return;
		if (toIndex >= size())
			toIndex = size() - 1;

		ArrayChangeData event = new ArrayChangeData();
		for (int i = fromIndex; i < toIndex; i++)
			event.remove(this.get(i));
		super.removeRange(fromIndex, toIndex);
		fire(event);
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#set(int, java.lang.Object)
	 */
	@Override
	public T set(int index, T element) {
		T result = super.set(index, element);
		fire(new ArrayChangeData().remove(result).add(element));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection c) {
		if (isEmpty() || c.isEmpty())
			return false;
		ArrayChangeData event = new ArrayChangeData<T>();
		for (Object o : c)
			if (contains(o))
				event.remove(o);
		boolean result = super.removeAll(c);
		fire(event);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection c) {
		if (isEmpty())
			return false;
		ArrayChangeData event = new ArrayChangeData<T>();
		for (Object o : this)
			if (!c.contains(o))
				event.remove(o);
		boolean result = super.retainAll(c);
		fire(event);
		return result;
	}
	
	@Override
	public int hashCode() {
		/*
		 * We MUST override hashcode because ProxyManager depends on HashMap to lookup the server ID
		 * for an object, but in Java the hashCode changes as you modify the collection; this means
		 * that mutable collections cannot be used as keys in maps.  The hashcode is generated during
		 * construction so that there is a kind of distribution of values (rather than just returning 
		 * 1 here) 
		 */
		return hashCode;
	}

	/**
	 * Fires an event
	 * @param event
	 */
	private void fire(ArrayChangeData event) {
		EventManager.fireDataEvent(this, "change", event);
		ProxyManager.collectionChanged(this, event);
	}
	
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
		}
		
	}

}