package com.jbkloh.ficha_buffet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jbkloh.ficha_buffet.model.IngredientesEntity;
 
public interface IngredientesRepository extends JpaRepository<IngredientesEntity, Long> {

@Query("SELECT i FROM IngredientesEntity i WHERE i.nome = :nome AND i.unidade = :unidade")
Optional<IngredientesEntity> findByNomeAndUnidadeMedida(
    @Param("nome") String nome, 
    @Param("unidade") String unidade
);

Optional<IngredientesEntity>findByNome(String nome);
} 
