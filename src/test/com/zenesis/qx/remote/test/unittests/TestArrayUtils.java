package com.zenesis.qx.remote.test.unittests;

import java.util.ArrayList;

import com.zenesis.qx.utils.ArrayUtils;

import junit.framework.TestCase;

public class TestArrayUtils extends TestCase {

	public void testMatchOrder() {
		ArrayList<String> col = new ArrayList(5);
		col.add("alpha");
		col.add("bravo");
		col.add("charlie");
		col.add("delta");
		col.add("echo");
		final String[] TEST1 = { "echo", "delta", "charlie", "bravo", "alpha" };
		ArrayUtils.matchOrder(col, TEST1);
		assertTrue(ArrayUtils.sameArray(col, TEST1));
		ArrayUtils.matchOrder(col, TEST1);
		assertTrue(ArrayUtils.sameArray(col, TEST1));

		final String[] TEST2 = { "echo", "delta", "alpha", "charlie", "bravo" };
		ArrayUtils.matchOrder(col, TEST2);
		assertTrue(ArrayUtils.sameArray(col, TEST2));
		
		// Test with gaps
		ArrayUtils.matchOrder(col, new String[] { "alpha", "foxtrot", "bravo", "george", "charlie" });
		assertTrue(ArrayUtils.sameArray(col, new String[] { "alpha", "bravo", "charlie", "echo", "delta" }));
		
		// Test with duplicate entries
		col.add("alpha");
		ArrayUtils.matchOrder(col, new String[] { "alpha", "delta", "charlie", "echo", "bravo" });
		assertTrue(ArrayUtils.sameArray(col, new String[] { "alpha", "delta", "charlie", "echo", "bravo", "alpha" }));
	}
}
