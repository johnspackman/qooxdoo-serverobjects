package com.zenesis.qx.remote.test.collections;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.HashMap;

public class TestQsoMap implements Proxied {
    
    public static class MyKey implements Proxied {
        @Property private String keyId;

        public MyKey(String keyId) {
            super();
            this.keyId = keyId;
        }

        public MyKey() {
            super();
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = ProxyManager.changeProperty(this, "keyId", keyId, this.keyId);
        }
        
        @Override
        public String toString() {
            return "KEYID:" + keyId;
        }
        
    }
    
    public static class MyValue implements Proxied {
        @Property private String valueId;

        public MyValue(String valueId) {
            super();
            this.valueId = valueId;
        }

        public MyValue() {
            super();
        }

        public String getValueId() {
            return valueId;
        }
        
        public void setValueId(String valueId) {
            this.valueId = ProxyManager.changeProperty(this, "valueId", valueId, this.valueId);
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
	
	@Method
	public void checkObjectMap() {
	    assertTrue(objectKeyMap.size() == 5);
        assertTrue(getValueId("delta").equals("four"));
        assertTrue(getValueId("echo").equals("five"));
	}
	
	private MyKey findKey(String id) {
	    for (MyKey key : objectKeyMap.keySet())
	        if (key.getKeyId().equals(id))
	            return key;
	    return null;
	}
	
    private String getValueId(MyKey key) {
        MyValue value = objectKeyMap.get(key);
        return value != null ? value.getValueId() : null;
    }

    private String getValueId(String key) {
        return getValueId(findKey(key));
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
