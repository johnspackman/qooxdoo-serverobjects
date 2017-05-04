package com.zenesis.qx.remote.test.collections;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.collections.ArrayList;

public class TestRecursiveArray implements Proxied {
    
    @Property
    private String id;

    @Property(arrayType=TestRecursiveArray.class, create=true)
    private ArrayList<TestRecursiveArray> children = new ArrayList();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = ProxyManager.changeProperty(this, "id", id, this.id);
    }

    public ArrayList<TestRecursiveArray> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<TestRecursiveArray> children) {
        this.children = ProxyManager.changeProperty(this, "children", children, this.children);
    }
    
}
