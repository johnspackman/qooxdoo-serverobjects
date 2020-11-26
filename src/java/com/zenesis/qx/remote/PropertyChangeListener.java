package com.zenesis.qx.remote;

/**
 * Classes which implement this are notified about changes to their properties
 * @author john
 *
 */
public interface PropertyChangeListener {

	/**
	 * Called when a property value changes
	 * @param property
	 * @param value
	 * @param oldValue
	 */
	public void propertyChanged(String propertyName, Object value, Object oldValue);
}
