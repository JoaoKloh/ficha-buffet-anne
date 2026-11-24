package com.jbkloh.ficha_buffet.repositories;

import com.jbkloh.ficha_buffet.model.ExtraEventoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtraEventoRepository extends JpaRepository<ExtraEventoEntity, Long> {
    List<ExtraEventoEntity> findByEventoId(Long eventoId);
}
