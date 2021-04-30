package com.zenesis.qx.remote.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.zenesis.qx.remote.ProxyTypeFactory;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxied {

  public Class<? extends ProxyTypeFactory> factory() default ProxyTypeFactory.class;
}
