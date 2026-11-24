package com.jbkloh.ficha_buffet.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.PlanoProducaoEntity;

public interface PlanoProducaoRepository extends JpaRepository<PlanoProducaoEntity, Long> {
    List<PlanoProducaoEntity> findByUnidade(String unidade);
}
