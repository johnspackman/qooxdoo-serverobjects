package com.zenesis.qx.remote.test.multiuser;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.test.properties.TestProperties;

public class TestThreading implements Proxied {

	@Method
	public TestProperties[] tryThis(TestProperties[] arr) {
		TestProperties[] result = new TestProperties[arr.length];
		for (int i = 0; i < arr.length; i++) {
			TestProperties tp = new TestProperties();
			tp.setQueued(arr[i].getWatchedString() + ":i");
		}
		return result;
	}
}
