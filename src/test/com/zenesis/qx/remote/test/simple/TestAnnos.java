package com.zenesis.qx.remote.test.simple;

import java.util.HashMap;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Annotation;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.ArrayList;

public class TestAnnos implements Proxied {

	@Annotation("\"qso.test.myAnno\"")
	@Property
	private String test;
	
	@Property(arrayType=String.class)
	private ArrayList<String> myStrings;
	
	@Property(arrayType=TestAnnos.class)
	private ArrayList<TestAnnos> myTestAnnos;
	
	@Property(keyType=String.class, arrayType=TestAnnos.class)
	private HashMap<String, TestAnnos> myTestAnnosMap;
	
	@Method(anno="qso.test.myMethodAnno")
	public void helloWorld() {
		
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = ProxyManager.changeProperty(this, "test", test, this.test);
	}

	/**
	 * @return the myStrings
	 */
	public ArrayList<String> getMyStrings() {
		return myStrings;
	}

	/**
	 * @return the myTestAnnos
	 */
	public ArrayList<TestAnnos> getMyTestAnnos() {
		return myTestAnnos;
	}

	/**
	 * @return the myTestAnnosMap
	 */
	public HashMap<String, TestAnnos> getMyTestAnnosMap() {
		return myTestAnnosMap;
	}
	
}
