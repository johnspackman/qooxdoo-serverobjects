/**
 * ************************************************************************
 * 
 *    server-objects - a contrib to the Qooxdoo project that makes server 
 *    and client objects operate seamlessly; like Qooxdoo, server objects 
 *    have properties, events, and methods all of which can be access from
 *    either server or client, regardless of where the original object was
 *    created.
 * 
 *    http://qooxdoo.org
 * 
 *    Copyright:
 *      2010 Zenesis Limited, http://www.zenesis.com
 * 
 *    License:
 *      LGPL: http://www.gnu.org/licenses/lgpl.html
 *      EPL: http://www.eclipse.org/org/documents/epl-v10.php
 *      
 *      This software is provided under the same licensing terms as Qooxdoo,
 *      please see the LICENSE file in the Qooxdoo project's top-level directory 
 *      for details.
 * 
 *    Authors:
 *      * John Spackman (john.spackman@zenesis.com)
 * 
 * ************************************************************************
 */
package com.zenesis.qx.remote.test.properties;

import java.util.ArrayList;

import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote.Toggle;

public class TestArrays implements ITestArrays {
	
	private String[] scalarArray = { "One", "Two", "Three", "Four", "Five" };
	private final ArrayList<String> scalarArrayList = new ArrayList<String>();
	private ITestValue[] objectArray = new ITestValue[scalarArray.length];
	private final ArrayList<ITestValue> objectArrayList = new ArrayList<ITestValue>();

	@Property(readOnly=Toggle.TRUE, arrayType=String.class)
	private ArrayList<String> readOnlyArray;

	public TestArrays() {
		super();
		for (String str : scalarArray)
			scalarArrayList.add(str);
		for (int i = 0; i < objectArray.length; i++) {
			objectArray[i] = new TestValue(i + 1);
			objectArrayList.add(new TestValue(i + 1));
		}
		readOnlyArray = new ArrayList<String>();
		readOnlyArray.add("peter");
		readOnlyArray.add("piper");
		readOnlyArray.add("picked");
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#getScalarArray()
	 */
	@Override
	public String[] getScalarArray() {
		return scalarArray;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#setScalarArray(java.lang.String[])
	 */
	@Override
	public void setScalarArray(String[] values) {
		this.scalarArray = values;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#getScalarArrayList()
	 */
	@Override
	public ArrayList<String> getScalarArrayList() {
		return scalarArrayList;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#testScalarArray(java.lang.String[])
	 */
	@Override
	public boolean testScalarArray(String[] values) {
		if (values.length != scalarArray.length)
			return false;
		for (int i = 0; i < values.length; i++)
			if (!values[i].equals(scalarArray[i]))
				return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#testScalarArrayList(java.lang.String[])
	 */
	@Override
	public boolean testScalarArrayList(String[] values) {
		if (values.length != scalarArrayList.size())
			return false;
		for (int i = 0; i < values.length; i++)
			if (!values[i].equals(scalarArrayList.get(i)))
				return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#getObjectArray()
	 */
	@Override
	public ITestValue[] getObjectArray() {
		return objectArray;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#setObjectArray(java.lang.ITestValue[])
	 */
	@Override
	public void setObjectArray(ITestValue[] values) {
		this.objectArray = values;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#getObjectArrayList()
	 */
	@Override
	public ArrayList<ITestValue> getObjectArrayList() {
		return objectArrayList;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#testObjectArray(java.lang.ITestValue[])
	 */
	@Override
	public boolean testObjectArray(ITestValue[] values) {
		if (values.length != objectArray.length)
			return false;
		for (int i = 0; i < values.length; i++)
			if (!values[i].equals(objectArray[i]))
				return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see com.zenesis.qx.remote.test.properties.ITestArrays#testObjectArrayList(java.lang.ITestValue[])
	 */
	@Override
	public boolean testObjectArrayList(ITestValue[] values) {
		if (values.length != objectArrayList.size())
			return false;
		for (int i = 0; i < values.length; i++)
			if (!values[i].equals(objectArrayList.get(i)))
				return false;
		return true;
	}

	/**
	 * @return the readOnlyArray
	 */
	public ArrayList<String> getReadOnlyArray() {
		return readOnlyArray;
	}
	
	@Method
	public boolean checkReadOnlyArray() {
		if (readOnlyArray.size() != 3)
			return false;
		return readOnlyArray.get(0).equals("peter") &&
				readOnlyArray.get(1).equals("piper") &&
				readOnlyArray.get(2).equals("picked");
	}
}
