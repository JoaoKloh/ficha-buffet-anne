package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Orçamento do evento: custos previstos que não saem das fichas técnicas. Não confundir com
 * OrcamentoEntity (proposta ao cliente). Uma linha por evento, criada ao salvar.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_orcamento")
public class OrcamentoEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    private EventoEntity evento;

    // Equipe prevista enquanto não há escala; nula se nunca foi informada.
    @Column(name = "equipe", precision = 15, scale = 2)
    private BigDecimal equipe;

    @Column(name = "transporte", nullable = false, precision = 15, scale = 2)
    private BigDecimal transporte = BigDecimal.ZERO;

    // Locação e outros (mobiliário, louça alugada, gelo, decoração a cargo do buffet).
    @Column(name = "outros", nullable = false, precision = 15, scale = 2)
    private BigDecimal outros = BigDecimal.ZERO;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;
}
