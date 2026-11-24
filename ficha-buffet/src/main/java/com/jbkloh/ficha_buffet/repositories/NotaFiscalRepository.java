package com.jbkloh.ficha_buffet.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.NotaFiscalEntity;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscalEntity, Long> {
    /** Só para chave preenchida: notas sem chave guardam texto vazio. */
    Optional<NotaFiscalEntity> findByChave(String chave);

    List<NotaFiscalEntity> findByEventoId(Long eventoId);
}
