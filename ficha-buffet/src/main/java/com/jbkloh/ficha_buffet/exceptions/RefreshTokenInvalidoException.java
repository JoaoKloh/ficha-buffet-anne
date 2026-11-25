package com.jbkloh.ficha_buffet.exceptions;

/**
 * Refresh token que não renova a sessão. O motivo exato (ausente, desconhecido, vencido,
 * revogado, reutilizado) fica só no servidor: a resposta é sempre REFRESH_TOKEN_INVALID.
 */
public class RefreshTokenInvalidoException extends RuntimeException {

    public RefreshTokenInvalidoException(String motivoInterno) {
        super(motivoInterno);
    }
}
