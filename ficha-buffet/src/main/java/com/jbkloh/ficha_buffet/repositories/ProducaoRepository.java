package com.jbkloh.ficha_buffet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.ProducaoEntity;

public interface ProducaoRepository extends JpaRepository<ProducaoEntity, Long> {

    
} 
