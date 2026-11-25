package com.jbkloh.ficha_buffet.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.jbkloh.ficha_buffet.model.UsuarioEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    // Fonte única da validade do access token — o cookie que o carrega (ver
    // AutenticacaoService) usa este mesmo valor como maxAge, para que o
    // cookie nunca sobreviva mais tempo do que o JWT que ele guarda. Vencido,
    // a sessão é renovada pelo refresh token (RefreshTokenService).
    private static final long EXPIRATION_MINUTES = 20;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public long getExpirationSeconds() {
        return EXPIRATION_MINUTES * 60;
    }

    public String extrairSubject(String token) {
        return jwtDecoder.decode(token).getSubject();
    }

    public String gerarToken(UsuarioEntity user) {
        return criarToken(user.getUsername(), user.getAuthorities());
    }

    public String criarToken(String email, Collection<? extends GrantedAuthority> authorities) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        List<String> roles = authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .distinct()
                        .collect(Collectors.toList());
                        
        JwtClaimsSet claims = JwtClaimsSet.builder()
                            .issuer("ficha-tecnica-auth-server")
                            .issuedAt(agora)
                            .expiresAt(expiracao)
                            .subject(email)
                            .claim("roles", roles) 
                            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
    }
