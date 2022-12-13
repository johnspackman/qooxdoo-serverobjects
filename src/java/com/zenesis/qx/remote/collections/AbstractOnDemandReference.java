package com.zenesis.qx.remote.collections;

import java.lang.ref.SoftReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zenesis.core.HasUuid;

public abstract class AbstractOnDemandReference<T extends HasUuid> implements OnDemandReference<T> {

  private static final Logger log = LogManager.getLogger(AbstractOnDemandReference.class);
  
  private String uuid;
  private SoftReference<T> ref;
  
  public AbstractOnDemandReference() {
  }
  
  public AbstractOnDemandReference(String uuid) {
    this.uuid = uuid;
  }
  
  public AbstractOnDemandReference(HasUuid doc) {
    uuid = doc.getUuid();
    ref = new SoftReference<T>((T)doc);
  }
  
  public static <T extends HasUuid> boolean sameAs(OnDemandReference ref, T doc) {
    return sameAs(ref, doc.getUuid());
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
    T doc = getFromUuid(uuid, load);
    if (doc == null) {
      if (load)
        log.fatal("Cannot find document with UUID " + uuid + " in DocumentRef");
      return null;
    }
    ref = new SoftReference<>(doc);
    return doc;
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
    String uuid = obj.getUuid();
    if (uuid == this.uuid)
      return;
    if (uuid != null && this.uuid != null && uuid.equals(this.uuid))
      return;
    this.uuid = uuid;
    ref = new SoftReference<>(obj);
  }

  @Override
  public void clear() {
    uuid = null;
    ref = null;
  }

}
