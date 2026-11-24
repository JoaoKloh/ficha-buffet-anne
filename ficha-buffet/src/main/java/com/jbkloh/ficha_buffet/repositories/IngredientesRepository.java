package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.IngredientesEntity;
 
public interface IngredientesRepository extends JpaRepository<IngredientesEntity, Long> {

Optional<IngredientesEntity>findByNome(String nome);
} 
