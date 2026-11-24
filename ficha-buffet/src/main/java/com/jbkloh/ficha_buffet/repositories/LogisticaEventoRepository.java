package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.LogisticaEventoEntity;

public interface LogisticaEventoRepository extends JpaRepository<LogisticaEventoEntity, Long> {
    Optional<LogisticaEventoEntity> findByEventoId(Long eventoId);
}
