package com.zenesis.qx.remote.test.collections;

import java.util.ArrayList;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;

public class TestJavaUtilArrayContainer implements Proxied {
	
	public static class Child implements Proxied {
		@Property
		public String name;
	}

	@Property(arrayType = Child.class)
	private ArrayList<Child> list;

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
	}
	
}
