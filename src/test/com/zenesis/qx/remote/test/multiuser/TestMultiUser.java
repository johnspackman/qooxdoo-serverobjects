package com.zenesis.qx.remote.test.multiuser;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.ProxySessionTracker;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.ArrayList;
import com.zenesis.qx.remote.collections.HashMap;

public class TestMultiUser implements Proxied {
	
	public static final class Status {
		public int numReady;
		public int yourIndex;
	}

	private static HashMap<ProxySessionTracker, Status> trackers = new HashMap<ProxySessionTracker, TestMultiUser.Status>();
	private static int numReady = 0;
	private static HashMap<String, String> stringMap = new HashMap<String, String>();
	private static ArrayList<String> stringArray = new ArrayList<String>();
	
	@Method
	public Status checkReady() {
		ProxySessionTracker tracker = ProxyManager.getTracker();
		if (trackers.isEmpty())
			resetAll();
		Status status = trackers.get(tracker);
		if (status == null) {
			status = new Status();
			trackers.put(tracker, status);
		}
		status.numReady = trackers.size();
		return status;
	}
	
	@Method
	public Status startTest() {
		int index = 1;
		for (Status status : trackers.values())
			status.yourIndex = index++;
		
		numReady++;
		ProxySessionTracker tracker = ProxyManager.getTracker();
		return trackers.get(tracker);
	}
	
	@Method
	public void syncUsers() {
		/*
		while (numReady != trackers.size())
			try {
				Thread.sleep(250);
			} catch(InterruptedException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			*/
	}
	
	@Property(arrayType=String.class, keyType=String.class)
	public HashMap<String, String> getStringMap() {
		return stringMap;
	}

	@Property(arrayType=String.class)
	public ArrayList<String> getStringArray() {
		return stringArray;
	}
	
	@Method
	public void noop() {
		
	}

	@Method
	public synchronized void resetAll() {
		trackers.clear();
		numReady = 0;
		stringMap.clear();
		stringMap.put("alpha", "one");
		stringMap.put("bravo", "two");
		stringMap.put("charlie", "three");
		stringMap.put("delta", "four");
		stringMap.put("echo", "five");
		stringArray.clear();
		stringArray.add("alpha");
		stringArray.add("bravo");
		stringArray.add("charlie");
		stringArray.add("delta");
		stringArray.add("echo");
	}
	
}
