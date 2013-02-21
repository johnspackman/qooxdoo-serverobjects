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

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote;

/**
 * Tests array handling
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
@Properties({
	@Property(value="scalarArray", array=Remote.Array.WRAP),
	@Property(value="scalarArrayList", array=Remote.Array.WRAP, arrayType=String.class),
	@Property(value="objectArray", array=Remote.Array.WRAP),
	@Property(value="objectArrayList", array=Remote.Array.WRAP, arrayType=ITestValue.class)
	})
public interface ITestArrays extends Proxied {

	public String[] getScalarArray();
	public void setScalarArray(String[] values); 
	public boolean testScalarArray(String[] values);
	
	public ArrayList<String> getScalarArrayList(); 
	public boolean testScalarArrayList(String[] values);
	
	public ITestValue[] getObjectArray();
	public void setObjectArray(ITestValue[] values); 
	public boolean testObjectArray(ITestValue[] values);
	
	public ArrayList<ITestValue> getObjectArrayList(); 
	public boolean testObjectArrayList(ITestValue[] values);
	
}

