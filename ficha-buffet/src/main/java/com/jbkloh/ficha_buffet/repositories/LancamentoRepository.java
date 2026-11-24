package com.jbkloh.ficha_buffet.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.LancamentoEntity;

public interface LancamentoRepository extends JpaRepository<LancamentoEntity, Long> {
    List<LancamentoEntity> findByEventoId(Long eventoId);

    List<LancamentoEntity> findByCategoriaId(Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);

    Optional<LancamentoEntity> findByParcelaId(Long parcelaId);

    Optional<LancamentoEntity> findByExtraId(Long extraId);

    List<LancamentoEntity> findByNotaFiscalIdOrderByParcelaNFAsc(Long notaFiscalId);
}
