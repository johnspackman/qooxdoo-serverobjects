package com.zenesis.qx.remote.test.simple;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;

public class TestAnnos implements Proxied {

	@Property(anno="qso.test.myAnno")
	private String test;
	
	@Method(anno="qso.test.myMethodAnno")
	public void helloWorld() {
		
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = ProxyManager.changeProperty(this, "test", test, this.test);
	}
	
}
