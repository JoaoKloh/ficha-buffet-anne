package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.ProducaoEventoEntity;

public interface ProducaoEventoRepository extends JpaRepository<ProducaoEventoEntity, Long> {
    Optional<ProducaoEventoEntity> findByEventoId(Long eventoId);
}
