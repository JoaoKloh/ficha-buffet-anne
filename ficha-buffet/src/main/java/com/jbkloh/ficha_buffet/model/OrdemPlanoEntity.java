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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plano da gestão para uma ordem de produção: prazo, horário limite, setor e responsável.
 * Só existe para a ordem ajustada; sem linha, valem os padrões da ficha técnica.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_ordem_plano",
    uniqueConstraints = @UniqueConstraint(name = "uq_evento_ordem_plano_evento_chave", columnNames = { "evento_id", "chave" }))
public class OrdemPlanoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    // m_<item do cardápio>, f_<item>, s_<ficha> ou x_exp.
    @Column(name = "chave", nullable = false, length = 40)
    private String chave;

    // Dias antes do evento.
    @Column(name = "dn", nullable = false)
    private Integer dn;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "setor", nullable = false, length = 40)
    private String setor;

    @Column(name = "responsavel", nullable = false, length = 80)
    private String responsavel = "";

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;
}
