package org.example.exception;

/**
 * Operación no permitida por la configuración del sistema (403). Igual que las demás
 * excepciones, el message es una CLAVE de i18n que resuelve el GlobalExceptionHandler.
 */
public class ForbiddenException extends RuntimeException {

    private final transient Object[] args;

    public ForbiddenException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
