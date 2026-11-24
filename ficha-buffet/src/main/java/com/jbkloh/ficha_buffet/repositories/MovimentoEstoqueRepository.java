package com.jbkloh.ficha_buffet.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.MovimentoEstoqueEntity;

public interface MovimentoEstoqueRepository extends JpaRepository<MovimentoEstoqueEntity, Long> {
    boolean existsByIngredienteId(Long ingredienteId);

    List<MovimentoEstoqueEntity> findByNotaFiscalId(Long notaFiscalId);
}
