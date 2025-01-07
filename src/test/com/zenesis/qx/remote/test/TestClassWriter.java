package com.zenesis.qx.remote.test;

import com.zenesis.qx.remote.AbstractProxyType;
import com.zenesis.qx.remote.ClassWriter;
import com.zenesis.qx.remote.ProxyTypeManager;
import com.zenesis.qx.remote.test.simple.TestScalars;

public class TestClassWriter {

	public static void main(String[] args) throws Exception {
		AbstractProxyType type = (AbstractProxyType)ProxyTypeManager.INSTANCE.getProxyType(TestScalars.class);
		ClassWriter cw = type.write();
		System.out.println(cw.getClassCode());
	}
}
