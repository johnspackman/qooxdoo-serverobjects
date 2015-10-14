package com.zenesis.qx.remote.test.collections;

import java.util.Collection;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.ArrayList;

public class TestQsoArrayList implements Proxied {

	@Property(arrayType=String.class)
	private ArrayList<String> stringArray = new ArrayList();
	
	public TestQsoArrayList() {
		stringArray.add("alpha");
		stringArray.add("bravo");
		stringArray.add("charlie");
		stringArray.add("delta");
		stringArray.add("echo");
	}

	public ArrayList<String> getStringArray() {
		return stringArray;
	}
	
	@Method
	public void makeChanges() {
		final String[] EXPECTED = { "alpha", "bravo", "echo", "foxtrot", "george" };
		compare(EXPECTED, stringArray);
		stringArray.add("henry");
		stringArray.add("indigo");
	}
	
	private void compare(Object[] expected, Collection actual) {
		if (expected.length != actual.size())
			throw new IllegalStateException("Wrong size");
		int i = 0;
		for (Object a : actual) {
			Object e = expected[i++];
			if (a == null && e == null)
				continue;
			if (a != null && e != null) {
				if (!a.equals(e))
					throw new IllegalStateException("Array differs");
				continue;
			}
			throw new IllegalStateException("Array differs");
		}
	}
}
