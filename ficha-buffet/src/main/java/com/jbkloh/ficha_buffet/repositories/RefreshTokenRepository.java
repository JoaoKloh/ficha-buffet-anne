package com.jbkloh.ficha_buffet.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jbkloh.ficha_buffet.model.RefreshTokenEntity;
import com.jbkloh.ficha_buffet.model.RefreshTokenEntity.MotivoRevogacao;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findBySessaoId(UUID sessaoId);

    /**
     * Revoga o token só se ainda estiver ativo. Devolve 1 para quem o consumiu e 0 para uma
     * requisição concorrente com o mesmo token: no PostgreSQL a segunda espera o bloqueio da
     * linha e, ao reavaliar o WHERE, já o encontra revogado.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenEntity t set t.revogadoEm = :agora, t.motivoRevogacao = :motivo "
        + "where t.id = :id and t.revogadoEm is null")
    int revogarSeAtivo(@Param("id") Long id, @Param("agora") Instant agora, @Param("motivo") MotivoRevogacao motivo);

    /** Revoga os tokens ainda ativos de uma sessão (um navegador); as demais sessões do usuário seguem. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenEntity t set t.revogadoEm = :agora, t.motivoRevogacao = :motivo "
        + "where t.sessaoId = :sessaoId and t.revogadoEm is null")
    int revogarSessao(@Param("sessaoId") UUID sessaoId, @Param("agora") Instant agora, @Param("motivo") MotivoRevogacao motivo);

    /** Limpeza: tokens vencidos do usuário já não servem nem para detectar reuso. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshTokenEntity t where t.usuario.id = :usuarioId and t.expiraEm < :agora")
    int apagarVencidos(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);
}
