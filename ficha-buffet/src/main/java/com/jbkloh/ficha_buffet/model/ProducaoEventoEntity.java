package com.jbkloh.ficha_buffet.model;

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
 * Mapa de produção do evento (ficha mestre). Existe a partir de "Gerar mapa"; as ordens
 * em si não são gravadas, saem do cardápio confirmado e das fichas na leitura.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_producao")
public class ProducaoEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    private EventoEntity evento;

    @Column(name = "gerado_em", nullable = false)
    private Instant geradoEm = Instant.now();

    @Column(name = "gerado_por", length = 100)
    private String geradoPor;

    // Nulo: só a gestão vê as ordens (em teste).
    @Column(name = "liberado_em")
    private Instant liberadoEm;

    @Column(name = "responsavel", nullable = false, length = 120)
    private String responsavel = "";

    @Column(name = "horarios", nullable = false, length = 300)
    private String horarios = "";
}
