package com.zenesis.qx.remote.test.simple;

import com.zenesis.qx.remote.LogEntry;
import com.zenesis.qx.remote.LogEntrySink;
import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.test.collections.TestQsoArrayList;
import com.zenesis.qx.remote.test.collections.TestQsoMap;
import com.zenesis.qx.remote.test.multiuser.TestMultiUser;
import com.zenesis.qx.remote.test.multiuser.TestThreading;

public class TestBootstrap implements Proxied, LogEntrySink {
	
	@Property
	private TestMultiUser multiUser = new TestMultiUser();

	@Property
	private TestThreading threadTest = new TestThreading();

	@Override
	public void addLogEntries(LogEntry[] entries) {
		for (LogEntry entry : entries)
			System.out.println("::CLIENT:: " + entry.toString());
	}

	@Method
	public Object getMainTests() {
		return new MainTests();
	}

	@Method
	public Object getArrayListTests() {
		return new TestQsoArrayList();
	}
	
	@Method
	public Object getMapTests() {
		return new TestQsoMap();
	}

	public TestMultiUser getMultiUser() {
		return multiUser;
	}

	public TestThreading getThreadTest() {
		return threadTest;
	}

}
