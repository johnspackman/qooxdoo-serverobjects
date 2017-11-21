package com.zenesis.qx.remote.test.simple;

import java.util.ArrayList;

import com.zenesis.qx.remote.Proxied;

import com.zenesis.qx.remote.ProxiedObserver;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.ProxyProperty;
import com.zenesis.qx.remote.annotations.EnclosingThisMethod;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;

public class TestObserver implements Proxied, ProxiedObserver {
    
    public static class StaticInner1 implements Proxied {
        private final TestObserver outer;
        @Property private boolean dirty;
        @Property public String someValue;
        
        public StaticInner1(TestObserver outer) {
            super();
            this.outer = outer;
        }
        
        @Method
        public void doStuff() {}

        public boolean isDirty() {
            return dirty;
        }

        public void setDirty(boolean dirty) {
            this.dirty = ProxyManager.changeProperty(this, "dirty", dirty, this.dirty);
        }

        public TestObserver getOuter() {
            return outer;
        }
    }
    
    public static class StaticInner2 implements Proxied {
        private final TestObserver outer;
        @Property() public boolean dirty;
        @Property public String someValue;
        
        public StaticInner2(TestObserver outer) {
            super();
            this.outer = outer;
        }

        @Method
        public void doStuff() {}

        public boolean isDirty() {
            return dirty;
        }

        public void setDirty(boolean dirty) {
            this.dirty = ProxyManager.changeProperty(this, "dirty", dirty, this.dirty);
        }

        @EnclosingThisMethod
        public TestObserver someMethod() {
            return outer;
        }
    }
    
    public class Inner implements Proxied {
        @Property() public boolean dirty;
        @Property public String someValue;
        @Property(arrayType=String.class) public ArrayList<String> simpleArray;
        @Property(arrayType=String.class) public com.zenesis.qx.remote.collections.ArrayList<String> qsoArrayList;
        
        public Inner() {
            super();
            this.simpleArray = new ArrayList();
            simpleArray.add("one");
            simpleArray.add("two");
            this.qsoArrayList = new com.zenesis.qx.remote.collections.ArrayList<>();
            qsoArrayList.add("three");
            qsoArrayList.add("four");
        }

        @Method
        public void doStuff() {}

        public boolean isDirty() {
            return dirty;
        }

        public void setDirty(boolean dirty) {
            this.dirty = ProxyManager.changeProperty(this, "dirty", dirty, this.dirty);
        }

        public ArrayList<String> getSimpleArray() {
            return simpleArray;
        }

        public com.zenesis.qx.remote.collections.ArrayList<String> getQsoArrayList() {
            return qsoArrayList;
        }

        public void setSimpleArray(ArrayList<String> simpleArray) {
            this.simpleArray = ProxyManager.changeProperty(this, "simpleArray", simpleArray, this.simpleArray);
        }

        public void setQsoArrayList(com.zenesis.qx.remote.collections.ArrayList<String> qsoArrayList) {
            this.qsoArrayList = ProxyManager.changeProperty(this, "qsoArrayList", qsoArrayList, this.qsoArrayList);
        }
    }

    @Method
    public StaticInner1 createStaticInner1() {
        return new StaticInner1(this);
    }

    @Method
    public StaticInner2 createStaticInner2() {
        return new StaticInner2(this);
    }

    @Method
    public Inner createInner() {
        return new Inner();
    }

    
    @Override
    public void observeSetProperty(Proxied proxied, ProxyProperty property, Object value, Object oldValue) {
        if (proxied instanceof StaticInner1)
            ((StaticInner1)proxied).setDirty(true);
        else if (proxied instanceof StaticInner2)
            ((StaticInner2)proxied).setDirty(true);
        else if (proxied instanceof Inner)
            ((Inner)proxied).setDirty(true);
        else 
            throw new IllegalStateException("Unexpected object passed to observeSetProperty: " + proxied);
    }

    @Override
    public void observeEditArray(Proxied proxied, ProxyProperty property, Object value) {
        if (proxied instanceof Inner)
            ((Inner)proxied).setDirty(true);
        else 
            throw new IllegalStateException("Unexpected object passed to observeEditArray: " + proxied);
    }
}
