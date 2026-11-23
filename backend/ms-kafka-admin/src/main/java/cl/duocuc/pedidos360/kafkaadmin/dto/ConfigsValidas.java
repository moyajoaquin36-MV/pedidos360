package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** El mapa de configuraciones solo trae claves de ConfigsTopico.PERMITIDAS con valores validos. */
@Documented
@Constraint(validatedBy = ConfigsValidasValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigsValidas {

    String message() default "configuraciones invalidas";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
