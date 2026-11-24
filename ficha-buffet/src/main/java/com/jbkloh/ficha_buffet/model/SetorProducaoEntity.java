package com.jbkloh.ficha_buffet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Setor da cozinha (cozinha quente, fria, confeitaria...). Renomeável e desativável, nunca excluído. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "setores_producao")
public class SetorProducaoEntity {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "nome", nullable = false, length = 60)
    private String nome;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "ordem", nullable = false)
    private int ordem;
}
