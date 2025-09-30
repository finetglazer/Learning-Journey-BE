package com.graduation.userservice.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = FieldsValueMatchValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldsValueMatch {

    // Default error message
    String message() default "Field values do not match!";

    // Field names to compare
    String field();
    String fieldMatch();

    // Required by validation spec
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // Allows for multiple annotations on the same class
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        FieldsValueMatch[] value();
    }
}