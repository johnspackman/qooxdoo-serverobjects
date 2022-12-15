package com.zenesis.qx.remote.collections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.zenesis.qx.remote.annotations.OnDemandReferenceFactoryType;

public abstract class OnDemandReferenceFactory<T> {
  
  private static final HashMap<Class, OnDemandReferenceFactory> s_factories = new HashMap<>();

  /**
   * Creates a reference
   * 
   * @param obj
   * @return
   */
  public abstract OnDemandReference<T> createReference(Object containerObject);
  
  /**
   * Creates a reference
   * 
   * @param <T>
   * @param object
   * @return
   */
  public static <T> OnDemandReference<T> createReferenceFor(Class referredClass, Object containerObject) {
    OnDemandReferenceFactory<T> factory;
    boolean noFactory = false;
    synchronized(s_factories) {
      noFactory = s_factories.containsKey(referredClass);
      factory = s_factories.get(referredClass);
    }
    if (factory == null) {
      if (noFactory)
        return null;
      
      for (Class clazz = referredClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
        OnDemandReferenceFactoryType anno = (OnDemandReferenceFactoryType)clazz.getAnnotation(OnDemandReferenceFactoryType.class);
        if (anno != null) {
          Class<OnDemandReferenceFactory> refClazz = anno.value();
          try {
            factory = (OnDemandReferenceFactory<T>) refClazz.newInstance();
          }catch(IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Cannot create instance of " + refClazz + ": " + e.getMessage());
          }
          break;
        }
      }
    }
    
    synchronized(s_factories) {
      s_factories.put(referredClass, factory);
    }
    if (factory == null)
      return null;
    OnDemandReference<T> ref = factory.createReference(containerObject);
    return ref;
  }
}
