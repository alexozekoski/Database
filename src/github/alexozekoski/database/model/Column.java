/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model;

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
public @interface Column {

    public String value() default "";

    public String name() default "";

    public boolean primary() default false;

    public boolean notnull() default false;

    public boolean update() default true;

    public boolean insert() default true;

    public boolean select() default true;

    public boolean fill() default true;

    public boolean json() default true;

    public boolean migration() default true;

    public String type() default "";

    public boolean unique() default false;

    public String foreign() default "";

    public String key() default "";

    public Class<? extends Serial> foreignKey() default Serial.class;

    public String defaultValue() default "";

    public boolean serial() default false;

    public String onDelete() default "";

    public int varchar() default -1;

    public boolean text() default false;

    public boolean index() default false;

    public Class listType() default Object.class;

    public String get() default "";

    public String set() default "";

    public boolean foreignInsert() default false;

    public boolean foreignUpdate() default false;

    public boolean foreignDelete() default false;

    public boolean foreignFill() default false;

    public boolean foreignOnlyObject() default true;

//    public boolean foreignUnique() default false;
    public boolean validate() default true;

    public int numeric() default -1;

    public int decimal() default -1;

    public boolean join() default false;
}
