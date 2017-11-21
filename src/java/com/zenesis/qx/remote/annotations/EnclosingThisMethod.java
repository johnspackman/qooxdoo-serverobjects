package com.zenesis.qx.remote.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in the Proxied object as the preferred method for getting the enclosing `this`;
 * only required for static classes where there is more than one "getXxxx" method
 *  
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnclosingThisMethod {

}
