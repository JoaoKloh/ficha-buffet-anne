package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
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

/** Extra cobrado no evento, além do contrato. O lançamento espelho aponta para ele (lancamentos_financeiros.extra_id). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "extras_evento")
public class ExtraEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao;

    @Column(name = "quantidade", nullable = false)
    private Double quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorUnitario;
}
