package com.jbkloh.ficha_buffet.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.LotePreparoEntity;

public interface LotePreparoRepository extends JpaRepository<LotePreparoEntity, Long> {
    List<LotePreparoEntity> findByProduzidoEm(LocalDate produzidoEm);

    boolean existsByPreparacaoId(Long preparacaoId);
}
