package com.zenesis.qx.remote.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Mixin {

  /**
   * The name of the mixin to include for this class
   * 
   * @return
   */
  public String value();

  /**
   * Whether to patch (false == to use 'include' method)
   * 
   * @return
   */
  public boolean patch() default true;
}
