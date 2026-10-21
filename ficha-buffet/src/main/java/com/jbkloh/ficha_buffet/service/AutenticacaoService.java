package com.jbkloh.ficha_buffet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.jbkloh.ficha_buffet.dtos.req.LoginRequestDTO;
import com.jbkloh.ficha_buffet.model.UsuarioEntity;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    public ResponseCookie login(LoginRequestDTO req) {
        var authToken = new UsernamePasswordAuthenticationToken(req.username(), req.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        UsuarioEntity usuario = (UsuarioEntity) authentication.getPrincipal();
        String jwtToken = tokenService.gerarToken(usuario);
        return ResponseCookie.from("accessToken", jwtToken)
                .httpOnly(true)
                .secure(cookieSecure) // true em produção (HTTPS) via property cookie.secure
                .path("/")
                .maxAge(tokenService.getExpirationSeconds()) // mesma validade do JWT — nunca deixa o cookie sobreviver ao token
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie logout() {
        return ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0) // instrui o browser a apagar o cookie imediatamente
                .sameSite("Strict")
                .build();
    }
}
