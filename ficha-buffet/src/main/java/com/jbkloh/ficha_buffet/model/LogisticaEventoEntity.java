package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import java.time.LocalTime;

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

/** Plano de logística e transporte do evento: veículos, motorista, viagens e horários. Uma linha por evento, criada ao salvar. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_logistica")
public class LogisticaEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    private EventoEntity evento;

    @Column(name = "veiculos", nullable = false, length = 200)
    private String veiculos = "";

    @Column(name = "motorista", nullable = false, length = 120)
    private String motorista = "";

    @Column(name = "viagens", nullable = false, length = 120)
    private String viagens = "";

    // Saída da fábrica.
    @Column(name = "saida")
    private LocalTime saida;

    // Início da montagem no local.
    @Column(name = "montagem")
    private LocalTime montagem;

    // Retorno previsto.
    @Column(name = "retorno")
    private LocalTime retorno;

    @Column(name = "obs", nullable = false, length = 2000)
    private String obs = "";

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;
}
