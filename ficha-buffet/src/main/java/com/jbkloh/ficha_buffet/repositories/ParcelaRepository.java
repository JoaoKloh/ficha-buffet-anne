package com.jbkloh.ficha_buffet.repositories;

import com.jbkloh.ficha_buffet.model.ParcelaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParcelaRepository extends JpaRepository<ParcelaEntity, Long> {
    List<ParcelaEntity> findByEventoIdOrderByNumeroAsc(Long eventoId);
}
