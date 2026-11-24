package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.FechamentoEventoEntity;

public interface FechamentoEventoRepository extends JpaRepository<FechamentoEventoEntity, Long> {
    Optional<FechamentoEventoEntity> findByEventoId(Long eventoId);
}
