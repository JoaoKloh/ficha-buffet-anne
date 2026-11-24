package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

/** Liberação do evento para expedição: conferências, horário de saída e quem autorizou. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_liberacao")
public class LiberacaoEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    private EventoEntity evento;

    // Item da liberação (Dominio.ITENS_LIBERACAO) -> conferido.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conferencias", nullable = false)
    private Map<String, Boolean> conferencias = new HashMap<>();

    @Column(name = "hora_saida")
    private LocalTime horaSaida;

    @Column(name = "autorizado_por", nullable = false, length = 80)
    private String autorizadoPor = "";

    @Column(name = "autorizado_em")
    private Instant autorizadoEm;

    @Column(name = "com_pendencias", nullable = false)
    private boolean comPendencias;

    @Column(name = "pendencias", nullable = false, length = 2000)
    private String pendencias = "";

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();
}
