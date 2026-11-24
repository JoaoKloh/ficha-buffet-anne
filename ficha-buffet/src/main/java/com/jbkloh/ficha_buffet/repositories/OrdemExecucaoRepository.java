package com.jbkloh.ficha_buffet.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.OrdemExecucaoEntity;

public interface OrdemExecucaoRepository extends JpaRepository<OrdemExecucaoEntity, Long> {
    List<OrdemExecucaoEntity> findByEventoId(Long eventoId);

    Optional<OrdemExecucaoEntity> findByEventoIdAndChave(Long eventoId, String chave);
}
