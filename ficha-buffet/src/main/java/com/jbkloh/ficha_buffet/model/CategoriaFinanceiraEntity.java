package com.jbkloh.ficha_buffet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Categoria de lançamento: uma linha da DRE dentro de um grupo. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "categorias_financeiras")
public class CategoriaFinanceiraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    // receita, deducao, custo_direto, despesa_variavel, despesa_fixa ou depreciacao_financeiro_ir.
    @Column(name = "grupo", nullable = false, length = 30)
    private String grupo;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    public CategoriaFinanceiraEntity(String nome, String grupo, Integer ordem) {
        this.nome = nome;
        this.grupo = grupo;
        this.ordem = ordem;
    }
}
