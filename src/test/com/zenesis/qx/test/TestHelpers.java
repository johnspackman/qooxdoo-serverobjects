package com.zenesis.qx.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.zenesis.qx.remote.Helpers;

public class TestHelpers {
	
	public enum TestEnum {
		PEOPLE_0_TO_10, HELLO_WORLD, PETER_PIPER_PICKED, R_AND_D_SERVICES
	}

	@Test
	public void test1() {
		String str = Helpers.enumToCamelCase(TestEnum.R_AND_D_SERVICES);
		String strEnum = Helpers.camelCaseToEnum(str);
		assertEquals(TestEnum.R_AND_D_SERVICES.toString(), strEnum);
	}

	@Test
	public void test() {
		Helpers.enumToCamelCase(TestEnum.HELLO_WORLD);
		for (TestEnum e : TestEnum.values()) {
			String str = Helpers.enumToCamelCase(e);
			String strEnum = Helpers.camelCaseToEnum(str);
			assertEquals(e.toString(), strEnum);
			TestEnum.valueOf(strEnum);
		}
	}

}
