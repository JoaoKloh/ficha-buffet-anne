package com.jbkloh.ficha_buffet.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Linha da receita: um item do cadastro ou uma sub-receita (outra ficha), nunca os dois. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "prato_ingrediente")
public class PratoIngredienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "preparacao_id", nullable = false)
    @JsonBackReference
    private PreparacaoEntity preparacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingrediente_id")
    private IngredientesEntity ingrediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_preparacao_id")
    private PreparacaoEntity subPreparacao;

    @Column(name = "quantidade", nullable = false)
    private Double quantidade;

    // Fator de correção da limpeza: 1 sem perda, 1,3 compra 30% a mais.
    @Column(name = "fc", nullable = false)
    private Double fc = 1.0;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;
}
