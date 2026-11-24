package com.jbkloh.ficha_buffet.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.PreparacaoEntity;

public interface PreparacaoRepository extends JpaRepository<PreparacaoEntity, Long> {
    /** Em quantas fichas o item do cadastro aparece na receita. */
    @Query("SELECT COUNT(DISTINCT pi.preparacao.id) FROM PratoIngredienteEntity pi WHERE pi.ingrediente.id = :ingredienteId")
    long contarFichasComIngrediente(@Param("ingredienteId") Long ingredienteId);

    @Query("SELECT COUNT(pi) > 0 FROM PratoIngredienteEntity pi WHERE pi.subPreparacao.id = :preparacaoId")
    boolean usadaComoSubReceita(@Param("preparacaoId") Long preparacaoId);
}
