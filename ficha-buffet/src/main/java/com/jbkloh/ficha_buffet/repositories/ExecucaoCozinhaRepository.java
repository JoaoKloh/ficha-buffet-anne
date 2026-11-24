package com.jbkloh.ficha_buffet.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.ExecucaoCozinhaEntity;

public interface ExecucaoCozinhaRepository extends JpaRepository<ExecucaoCozinhaEntity, Long> {
    List<ExecucaoCozinhaEntity> findByEventoId(Long eventoId);

    List<ExecucaoCozinhaEntity> findByItemCardapioId(Long itemCardapioId);
}
