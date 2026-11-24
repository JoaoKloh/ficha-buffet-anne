package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.PlanoProducaoItemEntity;

public interface PlanoProducaoItemRepository extends JpaRepository<PlanoProducaoItemEntity, Long> {
    boolean existsByPreparacaoId(Long preparacaoId);

    Optional<PlanoProducaoItemEntity> findByLoteId(Long loteId);
}
