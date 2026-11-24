package com.jbkloh.ficha_buffet.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.ItemCardapioEntity;

public interface ItemCardapioRepository extends JpaRepository<ItemCardapioEntity, Long> {
    List<ItemCardapioEntity> findByEventoId(Long eventoId);

    boolean existsByPreparacaoId(Long preparacaoId);

    boolean existsByEventoIdAndPreparacaoId(Long eventoId, Long preparacaoId);
}
