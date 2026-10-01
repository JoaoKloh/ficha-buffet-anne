package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.PermissoesUsuarioEntity;

public interface PermissoesUsuarioRepository extends JpaRepository<PermissoesUsuarioEntity, Long> {

    Optional<PermissoesUsuarioEntity>findByRole(String role);
    
}
