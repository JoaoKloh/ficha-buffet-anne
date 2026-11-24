package com.jbkloh.ficha_buffet.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Etapa do evento marcada à mão (feito) ou dispensada (só a degustação). As etapas que o
 * sistema detecta sozinho não são gravadas; a linha só existe enquanto feito ou dispensada.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_etapas", uniqueConstraints = @UniqueConstraint(name = "uq_evento_etapas_evento_etapa", columnNames = { "evento_id", "etapa" }))
public class EtapaEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    // Uma das 14 etapas de Dominio.ETAPAS_EVENTO.
    @Column(name = "etapa", nullable = false, length = 20)
    private String etapa;

    @Column(name = "feito", nullable = false)
    private boolean feito;

    @Column(name = "dispensada", nullable = false)
    private boolean dispensada;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;
}
