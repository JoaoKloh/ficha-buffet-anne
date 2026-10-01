package com.jbkloh.ficha_buffet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.PratoEntity;

public interface PratoRepository extends JpaRepository<PratoEntity, Long> {
    
}
