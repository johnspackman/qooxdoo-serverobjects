package com.zenesis.qx.remote.test.simple;

import com.zenesis.qx.remote.LogEntry;
import com.zenesis.qx.remote.LogEntrySink;
import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.test.collections.TestQsoArrayList;
import com.zenesis.qx.remote.test.collections.TestQsoMap;
import com.zenesis.qx.remote.test.collections.TestRecursiveArray;
import com.zenesis.qx.remote.test.multiuser.TestMultiUser;
import com.zenesis.qx.remote.test.multiuser.TestThreading;
import com.zenesis.qx.test.AbstractTestCase;

public class TestBootstrap implements Proxied, LogEntrySink {
	
	@Property private TestMultiUser multiUser = new TestMultiUser();
	@Property private TestThreading threadTest = new TestThreading();
	@Property private TestRecursiveArray recursiveArray;

	@Override
	public void addLogEntries(LogEntry[] entries) {
		for (LogEntry entry : entries)
			System.out.println("::CLIENT:: " + entry.toString());
	}

	@Method
	public Object getMainTests() {
		return new MainTests();
	}

	@Method
	public Object getArrayListTests() {
		return new TestQsoArrayList();
	}
	
	@Method
	public Object getMapTests() {
		return new TestQsoMap();
	}
	
	@Method
	public TestObserver getTestObserver() {
	    return new TestObserver();
	}

	public TestMultiUser getMultiUser() {
		return multiUser;
	}

	public TestThreading getThreadTest() {
		return threadTest;
	}

    public TestRecursiveArray getRecursiveArray() {
        return recursiveArray;
    }

    public void setRecursiveArray(TestRecursiveArray recursiveArray) {
        this.recursiveArray = ProxyManager.changeProperty(this, "recursiveArray", recursiveArray, this.recursiveArray);
    }

    @Method
    public void testRecursiveArray() {
        /*
        assertNotNull(recursiveArray);
        assertEquals(4, recursiveArray.getChildren().size());
        assertEquals("alpha", recursiveArray.getChildren().get(0).getId());
        assertEquals("bravo", recursiveArray.getChildren().get(1).getId());
        assertEquals("charlie", recursiveArray.getChildren().get(2).getId());
        assertEquals("delta", recursiveArray.getChildren().get(3).getId());
        
        TestRecursiveArray bravo = recursiveArray.getChildren().get(1);
        assertEquals(3, bravo.getChildren().size());
        assertEquals("bravo-alpha", bravo.getChildren().get(0).getId());
        assertEquals("bravo-bravo", bravo.getChildren().get(1).getId());
        assertEquals("bravo-charlie", bravo.getChildren().get(2).getId());
        */
    }
}
