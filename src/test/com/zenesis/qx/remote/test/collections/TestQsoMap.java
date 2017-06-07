package com.zenesis.qx.remote.test.collections;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.HashMap;

public class TestQsoMap implements Proxied {
    
    public class MyKey implements Proxied {
        @Property private final String keyId;

        public MyKey(String keyId) {
            super();
            this.keyId = keyId;
        }

        public String getKeyId() {
            return keyId;
        }

        @Override
        public int hashCode() {
            return keyId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return keyId.equals(((MyKey)obj).keyId);
        }

        @Override
        public String toString() {
            return "KEYID:" + keyId;
        }
        
    }
    
    public class MyValue implements Proxied {
        @Property private final String valueId;

        public MyValue(String valueId) {
            super();
            this.valueId = valueId;
        }

        public String getValueId() {
            return valueId;
        }
        
        @Override
        public int hashCode() {
            return valueId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return valueId.equals(((MyValue)obj).valueId);
        }

        @Override
        public String toString() {
            return "VALUEID:" + valueId;
        }
    }

	@Property(arrayType=String.class, keyType=String.class)
	public HashMap<String, String> stringMap = new HashMap<>();
	
	@Property(arrayType=MyValue.class, keyType=MyKey.class)
	public HashMap<MyKey, MyValue> objectKeyMap = new HashMap<>();
	
	public TestQsoMap() {
		stringMap.put("alpha", "one");
		stringMap.put("bravo", "two");
		stringMap.put("charlie", "three");
		stringMap.put("delta", "four");
		stringMap.put("echo", "five");
		
        objectKeyMap.put(new MyKey("alpha"), new MyValue("one"));
        objectKeyMap.put(new MyKey("bravo"), new MyValue("two"));
        objectKeyMap.put(new MyKey("charlie"), new MyValue("three"));
	}

	public HashMap<String, String> getStringMap() {
		return stringMap;
	}
	
	@Method
	public void makeChanges() {
		assertTrue(!stringMap.containsKey("bravo"));
		assertTrue(!stringMap.containsKey("delta"));
		assertTrue(stringMap.size() == 3);
		stringMap.put("alpha", "first again");
		stringMap.put("foxtrot", "six");
		stringMap.put("george", "seven");
	}
	
	public HashMap<MyKey, MyValue> getObjectKeyMap() {
        return objectKeyMap;
    }

    public void assertTrue(boolean value) {
		if (!value)
			throw new IllegalStateException();
	}
	public void assertTrue(boolean value, String msg) {
		if (!value)
			throw new IllegalStateException(msg);
	}
}
