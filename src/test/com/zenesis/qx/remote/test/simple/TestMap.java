package com.zenesis.qx.remote.test.simple;

import java.util.HashMap;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote.Array;

public class TestMap implements Proxied {
	
	public enum MyEnum {
		AARDVARK, BEETLE
	}

	private HashMap<String, String> stringMap = new HashMap<String, String>();
	private HashMap<MyEnum, TestMap> objectMap = new HashMap<TestMap.MyEnum, TestMap>();
	
	public TestMap() {
		stringMap.put("alpha", "one");
		stringMap.put("bravo", "two");
		stringMap.put("charlie", "three");
		objectMap.put(MyEnum.AARDVARK, this);
	}

	/**
	 * @return the objectMap
	 */
	@Property(arrayType=TestMap.class, keyType=MyEnum.class)
	public HashMap<MyEnum, TestMap> getObjectMap() {
		return objectMap;
	}
	
	@Method
	public void checkObjectMap() {
		if (objectMap.get(MyEnum.AARDVARK) != null)
			throw new IllegalStateException("ObjectMap not updated");
		if (objectMap.get(MyEnum.BEETLE) != this)
			throw new IllegalStateException("ObjectMap not updated");
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
