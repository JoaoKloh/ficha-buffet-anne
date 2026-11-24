package com.jbkloh.ficha_buffet.model;

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

/** Uso ou descarte de parte de um lote de pré-preparo. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lote_movimento")
public class LoteMovimentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_preparo_id", nullable = false)
    private LotePreparoEntity lote;

    // uso ou descarte.
    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "qtd", nullable = false)
    private Double qtd;

    // Uso: restaurante, evento ou outra. Descarte: vencido, qualidade, acidente ou sobra.
    @Column(name = "para", nullable = false, length = 15)
    private String para;

    // Uso em evento: qual evento (a ordem e o checklist dele deixam de baixar essa quantidade).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoEntity evento;

    @Column(name = "obs", nullable = false, length = 300)
    private String obs = "";

    @Column(name = "em", nullable = false)
    private Instant em = Instant.now();
}
