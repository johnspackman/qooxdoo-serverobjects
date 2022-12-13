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
  
  /**
   * Creates a new object, choosing a constructor that has a reference to the outer class 
   * if it is available 
   * 
   * @param <T>
   * @param type
   * @param outerObject
   * @return
   */
  private static <T> T newInstanceOf(Class<T> type, Object outerObject) {
    Class outerClass = outerObject != null ? outerObject.getClass() : null;
    Constructor noArgsCons = null;
    Constructor[] consList = type.getConstructors();
    T value;
    for (Constructor cons : consList) {
      Class[] paramTypes = cons.getParameterTypes();
      if (paramTypes.length == 1 && outerObject != null && paramTypes[0].isAssignableFrom(outerClass)) {
        try {
          value = (T) cons.newInstance(outerObject);
        }catch(IllegalAccessException | InstantiationException | InvocationTargetException e) {
          throw new IllegalStateException("Cannot create instance of " + type + ": " + e.getMessage());
        }
        return value;
      } else if (paramTypes.length == 0)
        noArgsCons = cons;
    }
    if (noArgsCons != null) {
      try {
        value = (T) noArgsCons.newInstance();
      }catch(IllegalAccessException | InstantiationException | InvocationTargetException e) {
        throw new IllegalStateException("Cannot create instance of " + type + ": " + e.getMessage());
      }
      return value;
    }
    return null;
  }
  
}
