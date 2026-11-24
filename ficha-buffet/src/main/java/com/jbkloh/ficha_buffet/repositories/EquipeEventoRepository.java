package com.jbkloh.ficha_buffet.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jbkloh.ficha_buffet.model.EquipeEventoEntity;

public interface EquipeEventoRepository extends JpaRepository<EquipeEventoEntity, Long> {

    @EntityGraph(attributePaths = { "pessoa", "lancamento" })
    List<EquipeEventoEntity> findByEventoIdOrderByIdAsc(Long eventoId);

    /** Escalas da pessoa com a conta a pagar da diária (para acompanhar a troca de nome). */
    @EntityGraph(attributePaths = { "evento", "lancamento" })
    List<EquipeEventoEntity> findByPessoaId(Long pessoaId);

    Optional<EquipeEventoEntity> findByIdAndEventoId(Long id, Long eventoId);

    boolean existsByEventoIdAndPessoaId(Long eventoId, Long pessoaId);
}
