package com.zenesis.qx.remote.collections;

public interface OnDemandReference<T> {

  public String getUuid();
  public T get(); 
  public T get(boolean load); 
  public void set(String uuid);
  public void set(T obj);
  public void clear();
}
