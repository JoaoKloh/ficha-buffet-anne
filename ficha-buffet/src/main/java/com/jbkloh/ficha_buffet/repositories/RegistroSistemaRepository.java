package com.jbkloh.ficha_buffet.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.RegistroSistemaEntity;

public interface RegistroSistemaRepository extends JpaRepository<RegistroSistemaEntity, String> {
}
