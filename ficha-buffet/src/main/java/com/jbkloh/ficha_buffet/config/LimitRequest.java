package com.jbkloh.ficha_buffet.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Rate limit por IP, aplicado apenas à rota pública de login — as demais rotas
 * exigem um JWT válido (ver SecurityConfig), então não precisam desse limite
 * aqui; tentativas de força bruta contra elas já esbarram na autenticação.
 *
 * Usa RateLimiterService (Bucket4j, refill automático em memória) em vez de um
 * Cache do Spring: não há CacheManager configurado neste projeto, então um
 * contador baseado em Cache nunca expiraria e bloquearia o IP permanentemente
 * após poucas tentativas.
 */
@Component
@RequiredArgsConstructor
public class LimitRequest extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isLoginAttempt = "POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getServletPath());

        if (isLoginAttempt) {
            Bucket bucket = rateLimiterService.resolveBucket(request.getRemoteAddr());

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                    "{\"error\": \"Muitas solicitações\", \"message\": \"Limite atingido. Tente novamente em instantes.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
