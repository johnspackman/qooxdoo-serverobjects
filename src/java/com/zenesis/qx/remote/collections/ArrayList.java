package com.zenesis.qx.remote.collections;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.AutoAttach;
import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.ProxyProperty;

/**
 * Provides an implementation of ArrayList which monitors changes to the array
 * and queues them for serialisation to the client via ProxyManager 
 * 
 * @author John Spackman
 */
@SuppressWarnings("serial")
public class ArrayList<T> extends java.util.ArrayList<T> implements AutoAttach {
	
	private static final Logger log = Logger.getLogger(ArrayList.class); 

	// The type of change
	public enum Type {
		ADD, REMOVE, ADD_REMOVE
	}
	
	public static final class ChangeData {
		
		public final Type type;
		public final int start;
		public final int end;
		public final Object[] added;
		public final Object[] removed;

		private ChangeData(Type type, int start, int end, Object[] added, Object[] removed) {
			super();
			this.type = type;
			this.start = start;
			this.end = end;
			this.added = added;
			this.removed = removed;
		}

		static ChangeData added(int start, Object added) {
			return new ChangeData(Type.ADD, start, start, new Object[] { added }, new Object[0]);
		}

		static ChangeData added(int start, int end, Object[] added) {
			return new ChangeData(Type.ADD, start, end, added, new Object[0]);
		}

		static ChangeData removed(int start, Object removed) {
			return new ChangeData(Type.REMOVE, start, start, new Object[0], new Object[] { removed });
		}

		static ChangeData removed(int start, int end, Object[] removed) {
			return new ChangeData(Type.REMOVE, start, end, new Object[0], removed);
		}

		static ChangeData replace(int start, Object added, Object removed) {
			return new ChangeData(Type.ADD_REMOVE, start, start, new Object[] { added}, new Object[] { removed });
		}

		static ChangeData replace(int start, int end, Object[] added, Object[] removed) {
			return new ChangeData(Type.ADD_REMOVE, start, end, added, removed);
		}
	}

	// The property we're connected to
	private Proxied proxiedObject;
	private ProxyProperty proxyProperty;

	/* (non-Javadoc)
	 * @see AutoAttach.setProxyProperty
	 */
	@Override
	public void setProxyProperty(Proxied proxiedObject, ProxyProperty proxyProperty) {
		if (this.proxiedObject != null && proxiedObject != null && this.proxiedObject != proxiedObject)
			throw new IllegalArgumentException("Cannot share instances of " + getClass() + " between Proxied objects, was=" + this.proxiedObject + ", new=" + proxiedObject);
		if (this.proxyProperty != null && proxyProperty != null && this.proxyProperty != proxyProperty)
			throw new IllegalArgumentException("Cannot share instances of " + getClass() + " between Proxied properties, was=" + this.proxyProperty + ", new=" + proxyProperty);
		this.proxiedObject = proxiedObject;
		this.proxyProperty = proxyProperty;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, T element) {
		super.add(index, element);
		fire(ChangeData.added(index, element));
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(T e) {
		boolean result = super.add(e);
		fire(ChangeData.added(size() - 1, e));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection c) {
		int len = this.size();
		boolean result = super.addAll(c);
		if (!result)
			return false;
		fire(ChangeData.added(len, this.size() - 1, c.toArray()));
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
		fire(ChangeData.added(index, index + c.size(), c.toArray()));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#clear()
	 */
	@Override
	public void clear() {
		Object[] removed = toArray();
		super.clear();
		if (removed.length != 0)
			fire(ChangeData.removed(0, removed.length, removed));
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(int)
	 */
	@Override
	public T remove(int index) {
		T result = super.remove(index);
		fire(ChangeData.removed(index, result));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		int index = indexOf(o);
		boolean result = super.remove(o);
		if (result)
			fire(ChangeData.removed(index, o));
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
		
		Object[] removed = new Object[toIndex - fromIndex];
		for (int i = 0; i < removed.length; i++)
			removed[i] = this.get(i + fromIndex);
		super.removeRange(fromIndex, toIndex);
		fire(ChangeData.removed(fromIndex, toIndex, removed));
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#set(int, java.lang.Object)
	 */
	@Override
	public T set(int index, T element) {
		T result = super.set(index, element);
		fire(ChangeData.replace(index, element, result));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection c) {
		ArrayList removed = new ArrayList<T>();
		int start = -1;
		int end = -1;
		for (Object obj : c) {
			int index = indexOf(obj);
			if (index > -1) {
				super.remove(obj);
				removed.add(obj);
				if (start == -1)
					start = index;
				end = index;
			}
		}
		if (!removed.isEmpty()) {
			fire(ChangeData.removed(start, end, removed.toArray()));
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection c) {
		ArrayList removed = new ArrayList<T>();
		int start = -1;
		int end = -1;
		for (int i = 0; i < size(); i++) {
			Object obj = get(i);
			if (!c.contains(obj)) {
				super.remove(obj);
				removed.add(obj);
				if (start == -1)
					start = i;
				end = i;
				i--;
			}
		}
		if (!removed.isEmpty()) {
			fire(ChangeData.removed(start, end, removed.toArray()));
			return true;
		}
		return false;
	}
	
	/**
	 * Fires an event
	 * @param event
	 */
	private void fire(ChangeData event) {
		EventManager.fireDataEvent(this, "change", event);
		if (proxyProperty != null)
			ProxyManager.propertyChanged(proxyProperty, proxiedObject, this, null);
	}
}