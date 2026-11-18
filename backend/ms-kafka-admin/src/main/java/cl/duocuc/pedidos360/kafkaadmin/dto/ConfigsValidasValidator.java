package cl.duocuc.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Junta todos los errores de configuracion en un solo mensaje para el campo "configs". */
public class ConfigsValidasValidator implements ConstraintValidator<ConfigsValidas, Map<String, String>> {

    @Override
    public boolean isValid(Map<String, String> configs, ConstraintValidatorContext context) {
        List<String> errores = ConfigsTopico.validar(configs);
        if (errores.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(escapar(String.join("; ", errores))).addConstraintViolation();
        return false;
    }

    /** El mensaje incluye claves enviadas por el usuario: se escapan para que no se interpreten como plantilla. */
    private static String escapar(String mensaje) {
        return mensaje.replaceAll("([\\\\{}$])", "\\\\$1");
    }
}
