package com.jbkloh.ficha_buffet.repositories;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.EventoEntity;

public interface EventoRepository extends JpaRepository<EventoEntity, Long> {
    /** Códigos já usados num ano ("26-%"), para gerar o próximo da sequência. */
    @Query("SELECT e.codigo FROM EventoEntity e WHERE e.codigo LIKE :prefixo")
    List<String> findCodigosPorPrefixo(@Param("prefixo") String prefixo);

    boolean existsByClienteId(Long clienteId);
}
