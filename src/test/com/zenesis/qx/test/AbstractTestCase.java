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
package com.zenesis.qx.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public abstract class AbstractTestCase extends TestCase {
	
	private boolean isSkippable(char c) {
		return Character.isWhitespace(c) || "\"'".indexOf(c) > -1;
	}
	
	/**
	 * Compares two strings ignoring white space
	 * @param actual
	 * @param expected
	 */
	protected void assertEqualsIgnoreSpace(String actual, String expected) {
		int iA = 0;
		int iE = 0;
		int lineA = 1;
		int lineE = 1;
		while (true) {
			char cA = 0;
			char cE = 0;
			while (iA < actual.length() && isSkippable(cA = actual.charAt(iA))) {
				if (cA == '\n')
					lineA++;
				iA++;
			}
			if (iA == actual.length())
				cA = 0;
			while (iE < expected.length() && isSkippable(cE = expected.charAt(iE))) {
				iE++;
				if (cE == '\n')
					lineE++;
			}
			if (iE == expected.length())
				cE = 0;
			if (cA == 0 && cE == 0)
				return;
			if (cA != cE)
				assertTrue("Unexpected output at line " + lineA + " in actual, " + lineE + " in expected", false);
			iA++;
			iE++;
		}
	}

	/**
	 * Compares a string with the contents of a text file which is located in the
	 * package directory of this class.
	 * @param actual
	 * @param filename
	 */
	protected void assertFromFile(String actual, String filename) {
		try {
			StringBuilder sb = new StringBuilder();
			try {
				InputStream is = getClass().getResourceAsStream(filename + ".txt");
				assertNotNull("Cannot find a file called " + filename + ".txt", is);
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = br.readLine()) != null)
					sb.append(line).append('\n');
				br.close();
			}catch(IOException e) {
				throw new IllegalStateException("Error while loading " + filename + ": " + e.getMessage(), e);
			}
			assertEqualsIgnoreSpace(sb.toString(), actual);
		}catch(AssertionFailedError e) {
			System.out.println("FAILED: " + e.getMessage() + ", FILE: " + filename + ", ACTUAL:\n" + actual);
			throw e;
		}
	}
}
