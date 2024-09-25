package com.zenesis.qx.remote.collections;

import java.lang.ref.SoftReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zenesis.core.HasUuid;

public abstract class AbstractOnDemandReference<T extends HasUuid> implements OnDemandReference<T> {

  private static final Logger log = LogManager.getLogger(AbstractOnDemandReference.class);
  
  protected String uuid;
  protected SoftReference<T> ref;
  private static ThreadLocal<Integer> s_stackDepth = new ThreadLocal<>();
  
  public AbstractOnDemandReference() {
  }
  
  public AbstractOnDemandReference(String uuid) {
    this.uuid = uuid;
  }
  
  public AbstractOnDemandReference(T doc) {
    uuid = getUuidFromObject(doc);
    ref = new SoftReference<T>((T)doc);
  }
  
  public boolean sameAs(T doc) {
    return sameAs(this, getUuidFromObject(doc));
  }
  
  public static boolean sameAs(OnDemandReference ref, String uuid) {
    String ru = ref != null ? ref.getUuid() : null;
    if (ru == null && uuid == null)
      return true;
    if (ru == null || uuid == null)
      return false;
    return ru.equals(uuid);
  }
  
  public boolean is(String uuid) {
    return this.uuid != null && this.uuid.equals(uuid);
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public T get(boolean load) {
    if (uuid == null)
      return null;
    if (ref != null) {
      T doc = (T)ref.get();
      if (doc != null)
        return doc;
    }
    synchronized(s_stackDepth) {
      Integer stackDepth = s_stackDepth.get();
      if (stackDepth == null)
        stackDepth = 1;
      else
        stackDepth++;
      s_stackDepth.set(stackDepth);
      if (stackDepth > 100) {
        throw new IllegalStateException("Stack Depth too deep getting on demand reference");
      }
    }
    try {
      T doc = getFromUuid(uuid, load);
      if (doc == null) {
        if (load)
          log.fatal("Cannot find document with UUID " + uuid + " in DocumentRef");
        return null;
      }
      ref = new SoftReference<>(doc);
      return doc;
    }finally {
      synchronized(s_stackDepth) {
        Integer stackDepth = s_stackDepth.get();
        s_stackDepth.set(stackDepth - 1);
      }
    }
  }
  
  protected abstract T getFromUuid(String uuid, boolean load);
  
  @Override
  public T get() {
    return get(true);
  }

  @Override
  public void set(String uuid) {
    if (uuid == this.uuid)
      return;
    if (uuid != null && this.uuid != null && uuid.equals(this.uuid))
      return;
    this.uuid = uuid;
    ref = null;
  }

  @Override
  public void set(T obj) {
    if (obj == null) {
      uuid = null;
      ref = null;
      return;
    }
    String uuid = getUuidFromObject(obj);
    if (uuid == this.uuid)
      return;
    if (uuid != null && this.uuid != null && uuid.equals(this.uuid))
      return;
    this.uuid = uuid;
    ref = new SoftReference<>(obj);
  }
  
  public String getUuidFromObject(T obj) {
    return obj.getUuid();
  }

  @Override
  public void clear() {
    uuid = null;
    ref = null;
  }

}
