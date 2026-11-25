package com.jbkloh.ficha_buffet.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jbkloh.ficha_buffet.dtos.req.LoginRequestDTO;
import com.jbkloh.ficha_buffet.exceptions.CodigoErroAutenticacao;
import com.jbkloh.ficha_buffet.exceptions.RefreshTokenInvalidoException;
import com.jbkloh.ficha_buffet.exceptions.dto.RestErrorMessage;
import com.jbkloh.ficha_buffet.service.AutenticacaoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody @Valid LoginRequestDTO req,
            @CookieValue(name = AutenticacaoService.COOKIE_RENOVACAO, required = false) String refreshTokenAnterior) {
        List<ResponseCookie> cookies = autenticacaoService.login(req, refreshTokenAnterior);
        return ResponseEntity.ok().headers(comCookies(cookies)).build();
    }

    /**
     * Renova a sessão: 200 com os dois cookies novos, ou 401 REFRESH_TOKEN_INVALID apagando
     * os cookies (token ausente, desconhecido, vencido, revogado ou reutilizado).
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<RestErrorMessage> refreshToken(
            @CookieValue(name = AutenticacaoService.COOKIE_RENOVACAO, required = false) String refreshToken) {
        try {
            List<ResponseCookie> cookies = autenticacaoService.renovar(refreshToken);
            return ResponseEntity.ok().headers(comCookies(cookies)).build();
        } catch (RefreshTokenInvalidoException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .headers(comCookies(autenticacaoService.cookiesApagados()))
                .body(new RestErrorMessage(HttpStatus.UNAUTHORIZED, CodigoErroAutenticacao.REFRESH_TOKEN_INVALID));
        }
    }

    /** Encerra a sessão deste navegador; sempre 200, mesmo sem sessão (idempotente). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AutenticacaoService.COOKIE_RENOVACAO, required = false) String refreshToken) {
        List<ResponseCookie> cookies = autenticacaoService.logout(refreshToken);
        return ResponseEntity.ok().headers(comCookies(cookies)).build();
    }

    private static HttpHeaders comCookies(List<ResponseCookie> cookies) {
        HttpHeaders headers = new HttpHeaders();
        cookies.forEach(c -> headers.add(HttpHeaders.SET_COOKIE, c.toString()));
        return headers;
    }
}
