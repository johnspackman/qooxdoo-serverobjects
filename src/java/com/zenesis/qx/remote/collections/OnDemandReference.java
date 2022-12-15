package com.zenesis.qx.remote.collections;

/**
 * On demand reference; every object has a UUID, which can be used to get an object on demand.
 * 
 * This is expected to be backed by a SoftReference or WeakReference to cache the object
 * 
 */
public interface OnDemandReference<T> {

  /**
   * The UUID of the referenced object, can be null
   * @return
   */
  public String getUuid();
  
  /**
   * Sets the UUID, can be null
   * @param uuid
   */
  public void set(String uuid);
  
  /**
   * Gets the object
   * 
   * @return
   */
  public T get();
  
  /**
   * Gets the object, but will only try and load it if `load` is true; if `load` is false then it
   * is accessed from the cache
   * @param load
   * @return
   */
  public T get(boolean load); 
  
  /**
   * Sets the object, the UUID will be obtained from the object and stored, the object will be cached
   * @param obj
   */
  public void set(T obj);
  
  /**
   * Sets the UUID/object to null
   */
  public void clear();
}
