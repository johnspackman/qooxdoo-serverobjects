package com.zenesis.qx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.zenesis.qx.remote.Helpers;
import com.zenesis.qx.remote.ProxySessionTracker;
import com.zenesis.qx.remote.test.simple.TestBootstrap;

public class TestHelpers {
	
	public enum TestEnum {
		PEOPLE_0_TO_10, HELLO_WORLD, PETER_PIPER_PICKED, R_AND_D_SERVICES
	}

	@Test
	public void test1() {
		ProxySessionTracker tracker = new ProxySessionTracker(TestBootstrap.class);
		String str = tracker.serialiseEnum(TestEnum.R_AND_D_SERVICES);
		String strEnum = tracker.deserialiseEnum(str);
		assertEquals(TestEnum.R_AND_D_SERVICES.toString(), strEnum);
	}

	@Test
	public void test() {
		ProxySessionTracker tracker = new ProxySessionTracker(TestBootstrap.class);
		tracker.serialiseEnum(TestEnum.HELLO_WORLD);
		for (TestEnum e : TestEnum.values()) {
			String str = tracker.serialiseEnum(e);
			String strEnum = tracker.deserialiseEnum(str);
			assertEquals(e.toString(), strEnum);
			TestEnum.valueOf(strEnum);
		}
	}

}
