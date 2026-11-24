package com.jbkloh.ficha_buffet.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.OcorrenciaEventoEntity;

public interface OcorrenciaEventoRepository extends JpaRepository<OcorrenciaEventoEntity, Long> {
    List<OcorrenciaEventoEntity> findByEventoId(Long eventoId);
}
