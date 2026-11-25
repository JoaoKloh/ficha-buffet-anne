package com.jbkloh.ficha_buffet.exceptions.dto;

import java.time.Instant;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.jbkloh.ficha_buffet.exceptions.CodigoErroAutenticacao;

/**
 * Corpo de erro da API. {@code code} só aparece nos erros de autenticação e autorização
 * (ver CodigoErroAutenticacao), para o frontend saber, por exemplo, se um 401 se resolve
 * renovando a sessão.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestErrorMessage(
    Instant timestamp,
    Integer status,
    String error,
    String message,
    String code
) {
    public RestErrorMessage(HttpStatus status, String message) {
        this(Instant.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public RestErrorMessage(HttpStatus status, CodigoErroAutenticacao codigo) {
        this(Instant.now(), status.value(), status.getReasonPhrase(), codigo.getMensagem(), codigo.name());
    }
}
