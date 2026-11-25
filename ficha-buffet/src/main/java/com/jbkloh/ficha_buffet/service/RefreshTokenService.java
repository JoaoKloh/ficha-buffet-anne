package com.jbkloh.ficha_buffet.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jbkloh.ficha_buffet.exceptions.RefreshTokenInvalidoException;
import com.jbkloh.ficha_buffet.model.RefreshTokenEntity;
import com.jbkloh.ficha_buffet.model.RefreshTokenEntity.MotivoRevogacao;
import com.jbkloh.ficha_buffet.model.UsuarioEntity;
import com.jbkloh.ficha_buffet.repositories.RefreshTokenRepository;
import com.jbkloh.ficha_buffet.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refresh tokens das sessões de login (tabela refresh_tokens, V13).
 *
 * <ul>
 * <li>Token: 32 bytes do SecureRandom em base64url (256 bits). Opaco, não é JWT.</li>
 * <li>Gravado só o SHA-256 em hex. Com 256 bits de entropia não há dicionário nem força bruta
 * possíveis, então um hash rápido e sem sal basta e permite a busca direta pelo índice único;
 * bcrypt/argon2 só fariam sentido para segredos de baixa entropia, como senhas.</li>
 * <li>Validade deslizante: cada renovação emite um token com mais 7 dias.</li>
 * <li>Rotação: o token usado é revogado (ROTACIONADO) e um novo entra na mesma sessão.</li>
 * <li>Reuso: um token já rotacionado reapresentado revoga a sessão toda (REUTILIZADO).
 * Uma sessão é um navegador; as outras sessões do usuário não são afetadas.</li>
 * </ul>
 *
 * As falhas de renovação gravam a revogação antes de lançar a exceção
 * ({@code noRollbackFor}), para a revogação por reuso valer mesmo com a resposta 401.
 * Nenhum token, bruto ou em hash, vai para o log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public static final Duration VALIDADE = Duration.ofDays(7);

    private static final int BYTES_TOKEN = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecureRandom random = new SecureRandom();

    /** Token emitido: o valor bruto (para o cookie) e o usuário dono da sessão. */
    public record Emitido(String token, UsuarioEntity usuario) {}

    /**
     * Abre uma sessão nova no login. Se o navegador ainda trazia o refresh token de uma sessão
     * anterior, ela é encerrada (NOVO_LOGIN) na mesma transação; sessões de outros
     * dispositivos ficam. Aproveita para apagar os tokens vencidos do usuário.
     */
    @Transactional
    public Emitido iniciarSessao(UsuarioEntity usuario, String tokenAnterior) {
        Instant agora = Instant.now();
        if (temValor(tokenAnterior)) {
            refreshTokenRepository.findByTokenHash(hash(tokenAnterior))
                .ifPresent(t -> refreshTokenRepository.revogarSessao(t.getSessaoId(), agora, MotivoRevogacao.NOVO_LOGIN));
        }
        refreshTokenRepository.apagarVencidos(usuario.getId(), agora);
        return new Emitido(gravar(usuario.getId(), UUID.randomUUID(), agora), usuario);
    }

    /**
     * Troca um refresh token válido por um novo na mesma sessão. O token apresentado é
     * revogado por um UPDATE condicional: entre duas requisições simultâneas com o mesmo
     * token, só uma consegue revogá-lo e a outra é tratada como reuso.
     */
    @Transactional(noRollbackFor = RefreshTokenInvalidoException.class)
    public Emitido rotacionar(String token) {
        if (!temValor(token)) {
            throw new RefreshTokenInvalidoException("ausente");
        }
        RefreshTokenEntity atual = refreshTokenRepository.findByTokenHash(hash(token))
            .orElseThrow(() -> new RefreshTokenInvalidoException("desconhecido"));
        Instant agora = Instant.now();
        Long usuarioId = atual.getUsuario().getId();
        UUID sessao = atual.getSessaoId();

        if (atual.getRevogadoEm() != null) {
            if (atual.getMotivoRevogacao() == MotivoRevogacao.ROTACIONADO) {
                revogarPorReuso(sessao, usuarioId, agora);
            }
            throw new RefreshTokenInvalidoException("revogado");
        }
        if (!atual.getExpiraEm().isAfter(agora)) {
            throw new RefreshTokenInvalidoException("vencido");
        }
        if (refreshTokenRepository.revogarSeAtivo(atual.getId(), agora, MotivoRevogacao.ROTACIONADO) == 0) {
            revogarPorReuso(sessao, usuarioId, agora);
            throw new RefreshTokenInvalidoException("consumido por outra requisição");
        }

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RefreshTokenInvalidoException("usuário removido"));
        return new Emitido(gravar(usuarioId, sessao, agora), usuario);
    }

    /** Logout: revoga a sessão do token apresentado. Token ausente ou desconhecido não é erro. */
    @Transactional
    public void encerrarSessao(String token) {
        if (!temValor(token)) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(token))
            .ifPresent(t -> refreshTokenRepository.revogarSessao(t.getSessaoId(), Instant.now(), MotivoRevogacao.LOGOUT));
    }

    private void revogarPorReuso(UUID sessao, Long usuarioId, Instant agora) {
        int revogados = refreshTokenRepository.revogarSessao(sessao, agora, MotivoRevogacao.REUTILIZADO);
        log.warn("Refresh token reutilizado: sessão revogada (usuário {}, {} token(s) ativo(s) revogado(s)).",
            usuarioId, revogados);
    }

    private String gravar(Long usuarioId, UUID sessao, Instant agora) {
        byte[] bytes = new byte[BYTES_TOKEN];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshTokenEntity entidade = new RefreshTokenEntity();
        entidade.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        entidade.setSessaoId(sessao);
        entidade.setTokenHash(hash(token));
        entidade.setCriadoEm(agora);
        entidade.setExpiraEm(agora.plus(VALIDADE));
        refreshTokenRepository.save(entidade);
        return token;
    }

    /** SHA-256 em hex (64 caracteres) do token bruto. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static boolean temValor(String token) {
        return token != null && !token.isBlank();
    }
}
