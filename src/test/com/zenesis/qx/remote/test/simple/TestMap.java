package com.zenesis.qx.remote.test.simple;

import java.util.HashMap;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote.Array;

public class TestMap implements Proxied {
	
	public enum Names {
		ALPHA, BRAVO, CHARLIE
	}
	public enum Numbers {
		ONE, TWO, THREE
	}

	private HashMap<String, String> stringMap = new HashMap<String, String>();
	
	@Property(arrayType=TestMap.class, keyType=Names.class)
	private HashMap<Names, TestMap> objectMap = new HashMap<TestMap.Names, TestMap>();
	
	@Property(arrayType=Numbers.class, keyType=Names.class)
	private HashMap<Names, Numbers> enumMap = new HashMap();
	
	public TestMap() {
		stringMap.put("alpha", "one");
		stringMap.put("bravo", "two");
		stringMap.put("charlie", "three");
		objectMap.put(Names.ALPHA, this);
		enumMap.put(Names.ALPHA, Numbers.ONE);
		enumMap.put(Names.BRAVO, Numbers.TWO);
	}

	public HashMap<Names, Numbers> getEnumMap() {
		return enumMap;
	}
	
	@Method
	public void checkEnumMap() {
		if (enumMap.containsKey(Names.ALPHA))
			throw new IllegalStateException("enumMap not updated");
		if (enumMap.get(Names.BRAVO) != Numbers.TWO)
			throw new IllegalStateException("enumMap not updated");
		if (enumMap.get(Names.CHARLIE) != Numbers.THREE)
			throw new IllegalStateException("enumMap not updated");
	}

	/**
	 * @return the objectMap
	 */
	public HashMap<Names, TestMap> getObjectMap() {
		return objectMap;
	}
	
	@Method
	public void checkObjectMap() {
		if (objectMap.get(Names.ALPHA) != null)
			throw new IllegalStateException("ObjectMap not updated");
		if (objectMap.get(Names.BRAVO) != this)
			throw new IllegalStateException("ObjectMap not updated");
	}

	public HashMap<String, String> getStringMap() {
		return stringMap;
	}

	/**
	 * @return the stringMap
	 */
	@Property(arrayType=String.class, array=Array.WRAP)
	public HashMap<String, String> getWrappedStringMap() {
		return stringMap;
	}

	/**
	 * @param stringMap the stringMap to set
	 */
	public void setWrappedStringMap(HashMap<String, String> stringMap) {
		this.stringMap = stringMap;
	}

	/**
	 * @return the stringMap
	 */
	@Property(arrayType=String.class, array=Array.NATIVE)
	public HashMap<String, String> getUnwrappedStringMap() {
		return stringMap;
	}

	/**
	 * @param stringMap the stringMap to set
	 */
	public void setUnwrappedStringMap(HashMap<String, String> stringMap) {
		this.stringMap = stringMap;
	}
	
	@Method
	public HashMap<String, String> getWrappedStringMapMethod() {
		return stringMap;
	}
	
	@Method
	public void checkMapUpdated() {
		String str = stringMap.get("bravo");
		if (str != null)
			throw new IllegalStateException("Map has not been updated");
		str = stringMap.get("charlie");
		if (str == null || !str.equals("three-changed"))
			throw new IllegalStateException("Map has not been updated");
		str = stringMap.get("delta");
		if (str == null || !str.equals("four"))
			throw new IllegalStateException("Map has not been updated");
	}
}
