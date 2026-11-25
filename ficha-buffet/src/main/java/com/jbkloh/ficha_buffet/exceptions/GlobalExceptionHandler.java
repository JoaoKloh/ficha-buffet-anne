package com.jbkloh.ficha_buffet.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.jbkloh.ficha_buffet.exceptions.dto.RestErrorMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * Erros no formato { message }: 400 para entrada malformada ou inválida, 404
 * para recurso inexistente, 409 para conflito com o estado atual e 422 para
 * regra de negócio não atendida.
 */
@Slf4j 
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<RestErrorMessage> handleAppException(AppException ex) {
        RestErrorMessage errorResponse = new RestErrorMessage(ex.getHttpStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    /** Falha do AuthenticationManager no login. 401/403 de rotas protegidas saem do RespostaErroSeguranca. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RestErrorMessage> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new RestErrorMessage(HttpStatus.UNAUTHORIZED, CodigoErroAutenticacao.INVALID_CREDENTIALS));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> response = new HashMap<>();

        // Obtém o primeiro erro de validação ocorrido (de campo ou, na falta, do objeto)
        FieldError fieldError = ex.getBindingResult().getFieldError();
        ObjectError globalError = ex.getBindingResult().getGlobalError();
        if (fieldError != null) {
            // Extrai somente a mensagem do erro (defaultMessage)
            response.put("message", fieldError.getDefaultMessage());
        } else if (globalError != null) {
            response.put("message", globalError.getDefaultMessage());
        } else {
            response.put("message", "Erro de validação nos dados enviados.");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /** Corpo ausente, JSON malformado ou tipo errado em algum campo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestErrorMessage> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new RestErrorMessage(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido."));
    }

    /** Parâmetro de rota ou de consulta com tipo errado (ex.: id não numérico). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RestErrorMessage> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new RestErrorMessage(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName() + "."));
    }

    /** Violação de chave estrangeira ou de unicidade que as regras do service não anteciparam. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RestErrorMessage> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new RestErrorMessage(HttpStatus.CONFLICT, "Este registro está em uso ou duplicado e não pode ser alterado desta forma."));
    }
}
