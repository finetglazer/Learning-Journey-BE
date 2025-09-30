package com.graduation.userservice.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.beanutils.BeanUtils;

public class FieldsValueMatchValidator implements ConstraintValidator<FieldsValueMatch, Object> {

    private String field;
    private String fieldMatch;
    private String message;

    @Override
    public void initialize(FieldsValueMatch constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldMatch = constraintAnnotation.fieldMatch();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            final Object firstObj = BeanUtils.getProperty(value, this.field);
            final Object secondObj = BeanUtils.getProperty(value, this.fieldMatch);

            // Both can be null, which we consider as valid
            boolean areEqual = (firstObj == null && secondObj == null) || (firstObj != null && firstObj.equals(secondObj));

            if (!areEqual) {
                // If the values are not equal, we disable the default violation
                // and build a new one on the 'fieldMatch' field.
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(fieldMatch)
                        .addConstraintViolation();
            }

            return areEqual;

        } catch (final Exception ignore) {
            // In case of any exception, we assume the validation is not applicable
        }
        return true;
    }
}