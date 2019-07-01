package com.zenesis.qx.remote.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyDate {

    public enum DateValues {
        DATE, DATE_TIME
    }
    public DateValues value() default DateValues.DATE_TIME;
    public boolean zeroTime() default true;
}
