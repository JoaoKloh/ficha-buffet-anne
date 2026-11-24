package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

/** Um pagamento ou recebimento de um lançamento: o lançamento pode ser quitado em partes. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lancamento_pagamentos")
public class LancamentoPagamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_id", nullable = false)
    private LancamentoEntity lancamento;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    // banco, banco_restaurante ou caixa.
    @Column(name = "conta", nullable = false, length = 20)
    private String conta = "banco";

    @Column(name = "forma", nullable = false, length = 15)
    private String forma = "";

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm = Instant.now();

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor = "";
}
