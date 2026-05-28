package org.example.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El message es una CLAVE de i18n (ej. "error.user.exists"); el GlobalExceptionHandler
 * la resuelve contra el MessageSource según el locale de la request. args alimenta los
 * placeholders {0}, {1}, ... del mensaje.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    private final transient Object[] args;

    public BadRequestException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}
