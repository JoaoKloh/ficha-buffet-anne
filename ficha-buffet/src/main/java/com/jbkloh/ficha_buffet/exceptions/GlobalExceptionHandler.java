package com.jbkloh.ficha_buffet.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jbkloh.ficha_buffet.exceptions.dto.RestErrorMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<RestErrorMessage> handleAppException(AppException ex) {
        RestErrorMessage errorResponse = new RestErrorMessage(ex.getHttpStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RestErrorMessage> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new RestErrorMessage(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> response = new HashMap<>();

        // Obtém o primeiro erro de validação ocorrido
        FieldError fieldError = ex.getBindingResult().getFieldError();

        if (fieldError != null) {
            // Extrai somente a mensagem do erro (defaultMessage)
            response.put("message", fieldError.getDefaultMessage());
        } else {
            response.put("message", "Erro de validação nos dados enviados.");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

}