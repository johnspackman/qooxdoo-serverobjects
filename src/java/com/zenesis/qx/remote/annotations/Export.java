package com.zenesis.qx.remote.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Export {

    public enum EXPORT {
        YES, NO, DYNAMIC
    }
    
    public EXPORT value() default EXPORT.YES;
}
