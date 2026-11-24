package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.OrcamentoEventoEntity;

public interface OrcamentoEventoRepository extends JpaRepository<OrcamentoEventoEntity, Long> {
    Optional<OrcamentoEventoEntity> findByEventoId(Long eventoId);
}
