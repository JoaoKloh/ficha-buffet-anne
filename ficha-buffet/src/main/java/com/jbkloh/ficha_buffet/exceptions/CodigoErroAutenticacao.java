package com.jbkloh.ficha_buffet.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Códigos do campo {@code code} nos erros de autenticação (401) e autorização (403).
 * Só ACCESS_TOKEN_EXPIRED leva o frontend a renovar a sessão; 403 nunca renova.
 */
@Getter
@RequiredArgsConstructor
public enum CodigoErroAutenticacao {

    /** 401 no login: usuário ou senha errados. */
    INVALID_CREDENTIALS("Usuário ou senha inválidos."),
    /** 401: rota protegida chamada sem access token. */
    AUTHENTICATION_REQUIRED("Faça login para continuar."),
    /** 401: access token vencido; o frontend renova a sessão e repete a requisição. */
    ACCESS_TOKEN_EXPIRED("Sua sessão expirou."),
    /** 401: access token malformado ou com assinatura inválida. */
    INVALID_TOKEN("Sessão inválida. Faça login novamente."),
    /** 401 na renovação: refresh token ausente, desconhecido, vencido, revogado ou reutilizado. */
    REFRESH_TOKEN_INVALID("Sua sessão expirou. Faça login novamente."),
    /** 403: autenticado, mas sem o papel exigido pela rota. */
    ACCESS_DENIED("Você não tem permissão para esta operação.");

    private final String mensagem;
}
