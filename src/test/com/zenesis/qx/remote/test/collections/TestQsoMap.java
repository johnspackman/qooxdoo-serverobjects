package com.zenesis.qx.remote.test.collections;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.HashMap;

public class TestQsoMap implements Proxied {

	@Property(arrayType=String.class, keyType=String.class)
	public HashMap<String, String> stringMap = new HashMap<String, String>();
	
	public TestQsoMap() {
		stringMap.put("alpha", "one");
		stringMap.put("bravo", "two");
		stringMap.put("charlie", "three");
		stringMap.put("delta", "four");
		stringMap.put("echo", "five");
	}

	public HashMap<String, String> getStringMap() {
		return stringMap;
	}
	
	@Method
	public void makeChanges() {
		assertTrue(!stringMap.containsKey("bravo"));
		assertTrue(!stringMap.containsKey("delta"));
		assertTrue(stringMap.size() == 3);
		stringMap.put("alpha", "first again");
		stringMap.put("foxtrot", "six");
		stringMap.put("george", "seven");
	}
	
	public void assertTrue(boolean value) {
		if (!value)
			throw new IllegalStateException();
	}
	public void assertTrue(boolean value, String msg) {
		if (!value)
			throw new IllegalStateException(msg);
	}
}
