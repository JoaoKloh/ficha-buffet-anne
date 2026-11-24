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

/** Louça ou utensílio que a ficha usa no evento. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "prato_louca")
public class PratoLoucaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preparacao_id", nullable = false)
    private PreparacaoEntity preparacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private IngredientesEntity ingrediente;

    @Column(name = "qtd", nullable = false)
    private Double qtd;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;
}
