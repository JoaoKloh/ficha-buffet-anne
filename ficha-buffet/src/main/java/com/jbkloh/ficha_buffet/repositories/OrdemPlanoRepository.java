package com.jbkloh.ficha_buffet.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.OrdemPlanoEntity;

public interface OrdemPlanoRepository extends JpaRepository<OrdemPlanoEntity, Long> {
    List<OrdemPlanoEntity> findByEventoId(Long eventoId);

    Optional<OrdemPlanoEntity> findByEventoIdAndChave(Long eventoId, String chave);
}
