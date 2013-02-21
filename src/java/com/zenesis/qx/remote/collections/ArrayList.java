package com.zenesis.qx.remote.collections;

import java.util.Collection;

import com.zenesis.qx.event.EventManager;
import com.zenesis.qx.remote.collections.ArrayChanges.Type;

/**
 * Provides an implementation of ArrayList which monitors changes to the array
 * and queues them for serialisation to the client via ProxyManager 
 * 
 * @author John Spackman
 */
public class ArrayList<T> extends java.util.ArrayList<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, T element) {
		super.add(index, element);
		fire(Type.ADD, index, index, element);
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(T e) {
		boolean result = super.add(e);
		fire(Type.ADD, -1, -1, e);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection c) {
		boolean result = super.addAll(c);
		fire(Type.ADD, -1, -1, c);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#addAll(int, java.util.Collection)
	 */
	@Override
	public boolean addAll(int index, Collection c) {
		boolean result = super.addAll(index, c);
		fire(Type.ADD, index, index, c);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#clear()
	 */
	@Override
	public void clear() {
		int size = size();
		super.clear();
		if (size != 0)
			fire(Type.DELETE, 0, size - 1, null);
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(int)
	 */
	@Override
	public T remove(int index) {
		T result = super.remove(index);
		fire(Type.DELETE, index, index, null);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		int index = indexOf(o);
		Boolean result = super.remove(o);
		if (index > -1)
			fire(Type.DELETE, index, index, null);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#removeRange(int, int)
	 */
	@Override
	public void removeRange(int fromIndex, int toIndex) {
		super.removeRange(fromIndex, toIndex);
		fire(Type.DELETE, fromIndex, toIndex, null);
	}

	/* (non-Javadoc)
	 * @see java.util.ArrayList#set(int, java.lang.Object)
	 */
	@Override
	public T set(int index, T element) {
		T result = super.set(index, element);
		fire(Type.REPLACE, index, index, element);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection c) {
		boolean changed = false;
		for (Object obj : c)
			if (remove(obj))
				changed = true;
		return changed;
	}

	/* (non-Javadoc)
	 * @see java.util.AbstractCollection#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection c) {
		boolean changed = super.retainAll(c);
		if (changed && size() > 0)
			fire(Type.REPLACE, 0, size() - 1, this);
		return changed;
	}
	
	private void fire(Type type, int startIndex, int endIndex, Object element) {
		EventManager.fireDataEvent(this, "change", new ArrayChanges.Change(type, startIndex, endIndex, element));
	}
	
	/*
	private void change(ArrayChanges.Type changeType, ArrayChanges.Change change) {
		ProxySessionTracker tracker = ProxyManager.getTracker();
		if (tracker == null)
			return;
		CommandQueue queue = tracker.getQueue();
		RequestHandler handler = tracker.getRequestHandler();
		if (handler != null && handler.isEditingCollection(this))
			return;
		ProxyType type = ProxyTypeManager.INSTANCE.getProxyType(keyObject.getClass());
		ProxyProperty property = type.getProperty(propertyName);
		if (property.isOnDemand())
			queue.queueCommand(CommandId.CommandType.EXPIRE, keyObject, propertyName, null);
		else
			queue.queueCommand(CommandId.CommandType.SET_VALUE, keyObject, propertyName, newValue);
		if (property.getEvent() != null)
			EventManager.getInstance().fireDataEvent(keyObject, property.getEvent().getName(), newValue);
	}*/
}
