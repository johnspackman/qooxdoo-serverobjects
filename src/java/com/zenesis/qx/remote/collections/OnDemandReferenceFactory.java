package com.zenesis.qx.remote.collections;

public abstract class OnDemandReferenceFactory<T> {

  public static OnDemandReferenceFactory INSTANCE;
  
  public abstract OnDemandReference<T> createReference(Object obj);
}
