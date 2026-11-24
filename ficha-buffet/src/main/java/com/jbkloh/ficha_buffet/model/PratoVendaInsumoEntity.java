package com.jbkloh.ficha_buffet.model;

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

/** Item do cadastro usado direto no prato de venda, com a quantidade por prato. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "prato_venda_insumo")
public class PratoVendaInsumoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prato_venda_id", nullable = false)
    private PratoVendaEntity pratoVenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private IngredientesEntity ingrediente;

    @Column(name = "qtd", nullable = false)
    private Double qtd;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;
}
