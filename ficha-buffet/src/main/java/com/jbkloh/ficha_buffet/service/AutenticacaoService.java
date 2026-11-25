package com.jbkloh.ficha_buffet.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.jbkloh.ficha_buffet.dtos.req.LoginRequestDTO;
import com.jbkloh.ficha_buffet.model.UsuarioEntity;

import lombok.RequiredArgsConstructor;

/**
 * Login, renovação e logout. A sessão vive em dois cookies httpOnly, SameSite=Strict e
 * Secure conforme cookie.secure: "accessToken" (JWT de 20 min, ver TokenService) e
 * "refreshToken" (opaco, 7 dias deslizantes, ver RefreshTokenService). Os dois usam path "/"
 * porque o BFF Next.js os recebe na própria origem e o middleware dele precisa ver o
 * refreshToken para não mandar ao /login quem só perdeu o access token.
 */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    public static final String COOKIE_ACESSO = "accessToken";
    public static final String COOKIE_RENOVACAO = "refreshToken";

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    /**
     * Confere usuário e senha e abre uma sessão nova. {@code refreshTokenAnterior} é o cookie
     * que o navegador ainda tinha (pode ser nulo): a sessão anterior dele é encerrada.
     */
    public List<ResponseCookie> login(LoginRequestDTO req, String refreshTokenAnterior) {
        var authToken = new UsernamePasswordAuthenticationToken(req.username(), req.password());
        Authentication authentication = authenticationManager.authenticate(authToken);
        UsuarioEntity usuario = (UsuarioEntity) authentication.getPrincipal();
        return cookiesDaSessao(refreshTokenService.iniciarSessao(usuario, refreshTokenAnterior));
    }

    /** Rotaciona o refresh token e emite um access token novo. Lança RefreshTokenInvalidoException. */
    public List<ResponseCookie> renovar(String refreshToken) {
        return cookiesDaSessao(refreshTokenService.rotacionar(refreshToken));
    }

    /** Revoga a sessão deste navegador (se houver) e devolve os cookies que a apagam. */
    public List<ResponseCookie> logout(String refreshToken) {
        refreshTokenService.encerrarSessao(refreshToken);
        return cookiesApagados();
    }

    /** Cookies vazios com maxAge 0 e os mesmos atributos dos originais: o navegador os apaga. */
    public List<ResponseCookie> cookiesApagados() {
        return List.of(cookie(COOKIE_ACESSO, "", 0), cookie(COOKIE_RENOVACAO, "", 0));
    }

    private List<ResponseCookie> cookiesDaSessao(RefreshTokenService.Emitido emitido) {
        String jwt = tokenService.gerarToken(emitido.usuario());
        return List.of(
            // mesma validade do JWT — nunca deixa o cookie sobreviver ao token
            cookie(COOKIE_ACESSO, jwt, tokenService.getExpirationSeconds()),
            cookie(COOKIE_RENOVACAO, emitido.token(), RefreshTokenService.VALIDADE.toSeconds()));
    }

    private ResponseCookie cookie(String nome, String valor, long maxAgeSegundos) {
        return ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(cookieSecure) // true em produção (HTTPS) via property cookie.secure
                .path("/")
                .maxAge(maxAgeSegundos)
                .sameSite("Strict")
                .build();
    }
}
