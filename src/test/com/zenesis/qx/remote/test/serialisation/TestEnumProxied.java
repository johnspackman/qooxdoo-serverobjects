package com.zenesis.qx.remote.test.serialisation;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.ProxyObjectMapper;
import com.zenesis.qx.remote.ProxySessionTracker;
import com.zenesis.qx.remote.test.simple.TestBootstrap;

public class TestEnumProxied {
	
	public enum MyEnum {
		A, B
	};
	
	public enum MyProxiedEnum implements Proxied {
		C, D
	};

	public static void main(String[] args) throws Exception {
		ProxySessionTracker tracker = new ProxySessionTracker(TestBootstrap.class);
		ProxyManager.selectTracker(tracker);
		ProxyObjectMapper mapper = new ProxyObjectMapper(tracker);
		ObjectWriter ow = mapper.writerWithDefaultPrettyPrinter();
		System.out.println(ow.writeValueAsString(MyEnum.A));
		System.out.println(ow.writeValueAsString(MyProxiedEnum.C));
	}
}
