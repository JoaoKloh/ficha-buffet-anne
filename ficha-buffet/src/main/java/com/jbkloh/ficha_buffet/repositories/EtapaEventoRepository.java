package com.jbkloh.ficha_buffet.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.EtapaEventoEntity;

public interface EtapaEventoRepository extends JpaRepository<EtapaEventoEntity, Long> {
    List<EtapaEventoEntity> findByEventoId(Long eventoId);

    Optional<EtapaEventoEntity> findByEventoIdAndEtapa(Long eventoId, String etapa);
}
