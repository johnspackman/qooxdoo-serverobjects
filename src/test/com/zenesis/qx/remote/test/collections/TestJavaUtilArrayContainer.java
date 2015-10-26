package com.zenesis.qx.remote.test.collections;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.ArrayList;
import com.zenesis.qx.remote.collections.HashMap;

public class TestJavaUtilArrayContainer implements Proxied {
	
	public static class Child implements Proxied {
		@Property
		public String name;
	}

	@Property(arrayType=Child.class) private ArrayList<Child> list;
	@Property(arrayType=String.class, keyType=String.class) private HashMap<String, String> map;

	public ArrayList<Child> getList() {
		return list;
	}

	public void setList(ArrayList<Child> list) {
		this.list = ProxyManager.changeProperty(this, "list", list, this.list);
	}

	@Method
	public void test() {
		if (list.size() != 3)
			throw new IllegalStateException("Unexpected list size");
		if (!list.get(0).name.equals("alpha"))
			throw new IllegalStateException("Unexpected list entry #0");
		if (!list.get(1).name.equals("bravo"))
			throw new IllegalStateException("Unexpected list entry #1");
		if (!list.get(2).name.equals("charlie"))
			throw new IllegalStateException("Unexpected list entry #2");
		if (map.size() != 3)
			throw new IllegalStateException("Unexpected map size");
		if (!map.get("alpha").equals("one"))
			throw new IllegalStateException("Unexpected map entry alpha");
		if (!map.get("bravo").equals("two"))
			throw new IllegalStateException("Unexpected map entry bravo");
		if (!map.get("charlie").equals("three"))
			throw new IllegalStateException("Unexpected map entry charlie");
	}

	public HashMap<String, String> getMap() {
		return map;
	}

	public void setMap(HashMap<String, String> map) {
		this.map = ProxyManager.changeProperty(this, "map", map, this.map);
	}
	
}
