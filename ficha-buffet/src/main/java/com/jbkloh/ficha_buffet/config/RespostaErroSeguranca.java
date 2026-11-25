package com.jbkloh.ficha_buffet.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.jbkloh.ficha_buffet.exceptions.CodigoErroAutenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Corpo JSON dos 401 e 403 que o Spring Security responde antes de chegar aos controllers
 * (sem ele saíam sem corpo). Mesmo formato do RestErrorMessage, com o campo {@code code}:
 * 401 ACCESS_TOKEN_EXPIRED (renovável), INVALID_TOKEN ou AUTHENTICATION_REQUIRED; 403
 * ACCESS_DENIED. O cabeçalho WWW-Authenticate do padrão Bearer continua sendo enviado.
 * Nenhum detalhe do token, da assinatura ou da chave vai para a resposta.
 */
@Component
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler bearerAccessDenied = new BearerTokenAccessDeniedHandler();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        bearerEntryPoint.commence(request, response, ex);
        escrever(response, HttpStatus.UNAUTHORIZED, classificar(ex));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        bearerAccessDenied.handle(request, response, ex);
        escrever(response, HttpStatus.FORBIDDEN, CodigoErroAutenticacao.ACCESS_DENIED);
    }

    /**
     * Token presente e vencido vira ACCESS_TOKEN_EXPIRED: o JwtTimestampValidator é a única
     * validação que reprova por "Jwt expired" (a assinatura já foi conferida antes dele).
     */
    static CodigoErroAutenticacao classificar(AuthenticationException ex) {
        if (!(ex instanceof InvalidBearerTokenException)) {
            return CodigoErroAutenticacao.AUTHENTICATION_REQUIRED;
        }
        for (Throwable c = ex.getCause(); c != null; c = c.getCause()) {
            if (c instanceof JwtValidationException jve && jve.getErrors().stream()
                    .anyMatch(e -> e.getDescription() != null && e.getDescription().startsWith("Jwt expired"))) {
                return CodigoErroAutenticacao.ACCESS_TOKEN_EXPIRED;
            }
        }
        return CodigoErroAutenticacao.INVALID_TOKEN;
    }

    private static void escrever(HttpServletResponse response, HttpStatus status, CodigoErroAutenticacao codigo)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Textos fixos do enum (sem aspas nem barras): dispensa o ObjectMapper aqui.
        response.getWriter().write("{\"timestamp\":\"" + Instant.now()
            + "\",\"status\":" + status.value()
            + ",\"error\":\"" + status.getReasonPhrase()
            + "\",\"message\":\"" + codigo.getMensagem()
            + "\",\"code\":\"" + codigo.name() + "\"}");
    }
}
