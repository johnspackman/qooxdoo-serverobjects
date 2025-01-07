package com.zenesis.qx.remote.test.multiuser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.test.properties.TestProperties;
import com.zenesis.qx.remote.test.properties.TestValue;

public class TestThreading implements Proxied {
	
	private static final Logger log = LogManager.getLogger();
	private int serial = 0;

	@Method
	public TestProperties[] tryThis(TestProperties[] arr) {
		TestProperties[] result = new TestProperties[arr.length];
		for (int i = 0; i < arr.length; i++) {
			TestProperties tp = new TestProperties();
			tp.setQueued(arr[i].getWatchedString() + ":i");
		}
		return result;
	}
	
	@Method
	public void resetSerial() {
		serial = 0;
	}
	
	@Method
	public int waitFor(long millis, TestValue value) {
		try {
			log.info("Starting wait for #" + serial + ", value=" + value + ": " + millis);
			Thread.sleep(millis);
			log.info("Ended wait for #" + serial + ", value=" + value + ": " + millis);
		} catch(InterruptedException e) {
			// Nothing
		}
		return this.serial++;
	}
	
}
