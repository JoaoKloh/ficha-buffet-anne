package com.jbkloh.ficha_buffet.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prato_ingrediente")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PratoIngredienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
@JoinColumn(name = "prato_id")
@JsonBackReference
private PratoEntity prato;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private IngredientesEntity ingrediente;

    @Column(name = "quantidade", nullable = false)
    private Double quantidade;
}