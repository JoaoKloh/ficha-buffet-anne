package com.jbkloh.ficha_buffet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.LancamentoEntity;

public interface LancamentoRepository extends JpaRepository<LancamentoEntity, Long> {

}
