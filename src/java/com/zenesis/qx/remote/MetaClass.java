package com.zenesis.qx.remote;

import java.util.Collection;
import java.util.Map;

import com.zenesis.qx.remote.annotations.Property;

/**
 * Because we support proxy-ing of actual, compiled Java classes as well as
 * virtual classes which are defined on the fly (e.g. reading RelaxNG schemas)
 * we need to have an abstraction of the Java Class.
 * 
 * @author john
 *
 */
public class MetaClass {

  private Class clazz;
  private Class collectionClass;
  private Class keyClass;
  private boolean isMap;
  private ProxyType proxyType;
  private boolean isArray;
  private boolean wrapArray;

  public MetaClass(Class clazz, Property anno) {
    if (clazz.isArray()) {
      this.clazz = clazz.getComponentType();
      isArray = true;

    } else if (Collection.class.isAssignableFrom(clazz)) {
      this.clazz = anno.arrayType();
      collectionClass = clazz;
      wrapArray = true;

    } else if (Map.class.isAssignableFrom(clazz)) {
      this.clazz = anno.arrayType();
      if (anno.keyType() != Object.class)
        this.keyClass = anno.keyType();
      collectionClass = clazz;
      isMap = true;
      wrapArray = true;

    } else {
      this.clazz = clazz;
    }
  }

  public boolean isArray() {
    return isArray;
  }

  /**
   * @param isArray the isArray to set
   */
  public void setArray(boolean isArray) {
    this.isArray = isArray;
  }

  public boolean isWrapArray() {
    return wrapArray;
  }

  public void setWrapArray(boolean wrapArray) {
    this.wrapArray = wrapArray;
  }

  public boolean isCollection() {
    return !isMap && collectionClass != null;
  }

  public boolean isMap() {
    return isMap && collectionClass != null;
  }

  public Class<? extends Collection> getCollectionClass() {
    return collectionClass;
  }

  /**
   * @param collectionClass the collectionClass to set
   */
  public void setCollectionClass(Class collectionClass) {
    this.collectionClass = collectionClass;
  }

  /**
   * @return the keyClass
   */
  public Class getKeyClass() {
    return keyClass;
  }

  public boolean isSubclassOf(Class clazz) {
    return clazz.isAssignableFrom(this.clazz);
  }

  public boolean isSuperclassOf(Class clazz) {
    return this.clazz.isAssignableFrom(clazz);
  }

  public Class getJavaType() {
    return clazz;
  }

  public void setJavaType(Class clazz) {
    if (clazz != this.clazz && collectionClass == null)
      throw new IllegalArgumentException("Cannot change the class in JavaClass unless the main type is a collection");
    this.clazz = clazz;
  }

  public ProxyType getProxyType() {
    if (proxyType == null && Proxied.class.isAssignableFrom(this.clazz))
      this.proxyType = ProxyTypeManager.INSTANCE.getProxyType(this.clazz);
    return proxyType;
  }

}
