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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ocorrência do evento: o que fugiu do planejado, quem registrou, o que foi feito e se já está resolvida. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_ocorrencias")
public class OcorrenciaEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    // Um dos tipos de Dominio.RE_TIPO_OCORRENCIA.
    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "quem", nullable = false, length = 80)
    private String quem;

    @Column(name = "texto", nullable = false, length = 2000)
    private String texto;

    @Column(name = "acao", nullable = false, length = 2000)
    private String acao = "";

    @Column(name = "quando", nullable = false)
    private Instant quando = Instant.now();

    @Column(name = "resolvida", nullable = false)
    private boolean resolvida;
}
