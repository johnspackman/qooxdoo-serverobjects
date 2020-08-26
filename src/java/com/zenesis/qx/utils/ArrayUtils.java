package com.zenesis.qx.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletException;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyProperty;

/**
 * Helper methods for dealing with arrays, collections, and maps
 * @author john
 *
 */
public class ArrayUtils {

	/**
	 * Gets a property value, creating it if necessary
	 * @param serverObject
	 * @param prop
	 * @return
	 * @throws ServletException
	 */
	public static Collection getCollection(Proxied serverObject, ProxyProperty prop) throws ServletException {
		Collection list = (Collection)prop.getValue(serverObject);
		if (list == null) {
			try {
				list = (Collection)prop.getPropertyClass().getCollectionClass().newInstance();
			}catch(Exception e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			prop.setValue(serverObject, list);
		}
		return list;		
	}
	
	/**
	 * Adds every item in an array into a collection; uses reflection so that arrays of primitives are supported
	 * @param col
	 * @param items
	 */
	public static void addAll(Collection col, Object items) {
		if (items != null) {
			int len = Array.getLength(items);
			for (int i = 0; i < len; i++) {
				Object item = Array.get(items, i);
				col.add(item);
			}
		}
	}
	
	/**
	 * Removes every item in an array from a collection; uses reflection so that arrays of primitives are supported
	 * @param col
	 * @param items
	 */
	public static void removeAll(Collection col, Object items) {
		if (items != null) {
			int len = Array.getLength(items);
			for (int i = 0; i < len; i++) {
				Object item = Array.get(items, i);
				col.remove(item);
			}
		}
	}

	/**
	 * Converts a collection into an array of a given class
	 * @param col
	 * @param clazz
	 * @return
	 */
	public static Object toArray(Collection col, Class clazz) {
		Object result = Array.newInstance(clazz, col.size());
		int i = 0;
		for (Object item : col)
			Array.set(result, i++, item);
		return result;
	}
	
	/**
	 * Tests to see if the collection and the array have the same (or equivalent) cotents)
	 * @param col
	 * @param array
	 * @return
	 */
	public static boolean sameArray(Collection col, Object array) {
		if (col == null && array == null)
			return true;
		if (col == null || array == null)
			return false;
		int len = Array.getLength(array);
		if (col.size() != len)
			return false;
		int i = 0;
		for (Object co : col) {
			Object ao = Array.get(array, i++);
			if (!same(co, ao))
				return false;
		}
		return true;
	}
	
	/**
	 * Tests to see if the two objects are the same
	 * @param left
	 * @param right
	 * @return
	 */
	public static boolean same(Object left, Object right) {
		if (left == right)
			return true;
		if (left == null || right == null)
			return false;
		return left.equals(right);
	}
	
	/**
	 * Removes an object from the ArrayList, only if it occurs after start
	 * @param arr the ArrayList to scan
	 * @param start index to start looking from
	 * @param obj the object to remove
	 * @return true if an element was removed
	 */
	public static boolean removeAfter(ArrayList arr, int start, Object obj) {
		for (; start < arr.size(); start++) {
			if (same(arr.get(start), obj)) {
				arr.remove(start);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Forces collection to match the order of the array, ignoring missing elements on either side.
	 * Elements in col which are not present in array have their ordering preserved but are placed
	 * at the end of col; elements in array which are not present in col are ignored. 
	 * @param col Collection to sort
	 * @param array Array with required order
	 */
	public static void matchOrder(Collection col, Object array) {
		int arrayLen = Array.getLength(array);
		ArrayList sorted = toArrayList(col);
		int colIndex = 0;
		for (int arrayIndex = 0; arrayIndex < arrayLen && arrayIndex < sorted.size(); arrayIndex++) {
			Object lo = sorted.get(colIndex);
			Object ao = Array.get(array, arrayIndex);
			if (same(ao, lo)) {
				colIndex++;
				continue;
			}
			if (removeAfter(sorted, colIndex + 1, ao)) {
				sorted.add(colIndex, ao);
				colIndex++;
				continue;
			}
		}
		if (col != sorted) {
			col.clear();
			col.addAll(sorted);
		}
	}
	
	/**
	 * Converts collection to an ArrayList, by type casting or by duplicating it
	 * @param col
	 * @return
	 */
	public static ArrayList toArrayList(Collection col) {
		if (col instanceof ArrayList)
			return (ArrayList)col;
		ArrayList result = new ArrayList();
		result.addAll(col);
		return result;
	}
	
	/**
	 * Simple helper to push a value onto the end of an Object[] array
	 * @param src the starting array, can be null
	 * @param value the value to add
	 * @return the new array
	 */
	public static Object[] addToObjectArray(Object[] src, Object value) {
		if (src == null || src.length == 0)
			return new Object[] { value };
		Object[] dest = new Object[src.length + 1];
		System.arraycopy(src, 0, dest, 0, src.length);
		dest[src.length] = value;
		return dest;
	}
	
	/**
	 * Simple helper to push a value onto the end of an Object[] array
	 * @param src the starting array, can be null
	 * @param value the value to add
	 * @return the new array
	 */
	public static <T> T[] addToArray(Class<T> clazz, T[] src, T value) {
		if (src == null || src.length == 0) {
			T[] result = (T[])Array.newInstance(clazz, 1);
			result[0] = value;
			return result;
		}
		
		T[] dest = (T[])Array.newInstance(clazz, src.length + 1);
		System.arraycopy(src, 0, dest, 0, src.length);
		dest[src.length] = value;
		return dest;
	}
	
	/**
	 * Gets a property value, creating it if necessary
	 * @param serverObject
	 * @param prop
	 * @return
	 * @throws ServletException
	 */
	public static Map getMap(Proxied serverObject, ProxyProperty prop) throws ServletException {
		Map map = (Map)prop.getValue(serverObject);
		if (map == null) {
			try {
				map = (Map)prop.getPropertyClass().getCollectionClass().newInstance();
			}catch(Exception e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			prop.setValue(serverObject, map);
		}
		return map;
	}
	
	/**
	 * Removes every item in an array from a map; uses reflection so that arrays of primitives are supported
	 * @param col
	 * @param items
	 */
	public static void removeAll(Map map, Object items) {
		if (items != null) {
			int len = Array.getLength(items);
			for (int i = 0; i < len; i++) {
				Object key = Array.get(items, i);
				map.remove(key);
			}
		}
	}
}
