package com.zenesis.qx.remote.collections;

import com.zenesis.qx.remote.CumulativeData;

/**
 * Stores the changes which have occured on the server so that they can be applied
 * on the client without requiring a complete refresh.
 * 
 * This class stores a single instance of a change unless add() or merge() is used
 * to append additional changes, at which point it switches to an ArrayList.
 * 
 * @author John Spackman
 */
public class ArrayChanges implements CumulativeData {
	
	// The type of change
	public enum Type {
		ADD, DELETE, REPLACE
	}
	
	/*
	 * An individual delta
	 */
	public static final class Change {
		public final Type type;
		public final int startIndex;
		public final int endIndex;
		public final Object value;
		
		/**
		 * @param type
		 * @param value
		 */
		public Change(Type type, int startIndex, int endIndex, Object value) {
			super();
			this.type = type;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.value = value;
		}
	}
	
	// The change, if there is only one
	private Change change;
	
	// The changes, if there are more than one
	private java.util.ArrayList<Change> changes;

	/**
	 * Constructor
	 * @param type
	 * @param obj
	 */
	public ArrayChanges(Type type, int startIndex, int endIndex, Object obj) {
		change = new Change(type, startIndex, endIndex, obj);
	}
	
	/**
	 * Adds an additional change
	 * @param type
	 * @param obj
	 */
	public void add(Type type, int startIndex, int endIndex, Object obj) {
		if (changes == null) {
			changes = new java.util.ArrayList<Change>();
			changes.add(change);
			change = null;
		}
		changes.add(new Change(type, startIndex, endIndex, obj));
	}
	
	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.CumulativeData#merge(com.zenesis.qx.remote.CumulativeData)
	 */
	@Override
	public void merge(CumulativeData data) {
		if (changes == null) {
			changes = new java.util.ArrayList<Change>();
			changes.add(change);
			change = null;
		}
		ArrayChanges that = (ArrayChanges)data;
		if (that.changes != null)
			changes.addAll(that.changes);
		else
			changes.add(that.change);
	}

	
}
