package com.local.lms.annotations;

import com.local.lms.domain.enums.Operator;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Searchable {

    String field() default ""; // entity field name (optional override)

    Operator operator() default Operator.EQUAL;

    boolean ignoreIfNull() default true;
}