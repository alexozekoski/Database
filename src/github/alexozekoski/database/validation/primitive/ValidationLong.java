/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation.primitive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author alexozekoski
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValidationLong {

    public long min() default Long.MIN_VALUE;

    public long max() default Long.MAX_VALUE;

    public long[] value() default {};
    
    public String minVolume() default "";
    
    public String maxVolume() default "";

}
