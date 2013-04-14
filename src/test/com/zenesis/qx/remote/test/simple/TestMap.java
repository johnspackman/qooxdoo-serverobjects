package com.zenesis.qx.remote.test.simple;

import java.util.HashMap;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Property;

public class TestMap implements Proxied {

	@Property(arrayType=String.class)
	private HashMap<String, String> stringMap = new HashMap<String, String>();
	
	public TestMap() {
		stringMap.put("alpha", "one");
		stringMap.put("bravo", "two");
		stringMap.put("charlie", "three");
	}

	/**
	 * @return the stringMap
	 */
	public HashMap<String, String> getStringMap() {
		return stringMap;
	}

	/**
	 * @param stringMap the stringMap to set
	 */
	public void setStringMap(HashMap<String, String> stringMap) {
		this.stringMap = stringMap;
	}
	
}
