package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.PratoVendaEntity;

public interface PratoVendaRepository extends JpaRepository<PratoVendaEntity, Long> {
    Optional<PratoVendaEntity> findByCodigoPdv(String codigoPdv);

    @Query("SELECT COUNT(c) > 0 FROM PratoVendaComponenteEntity c WHERE c.preparacao.id = :preparacaoId")
    boolean usaPreparacao(@Param("preparacaoId") Long preparacaoId);
}
