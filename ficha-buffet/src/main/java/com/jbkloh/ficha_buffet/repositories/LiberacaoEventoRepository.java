package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.LiberacaoEventoEntity;

public interface LiberacaoEventoRepository extends JpaRepository<LiberacaoEventoEntity, Long> {
    Optional<LiberacaoEventoEntity> findByEventoId(Long eventoId);
}
