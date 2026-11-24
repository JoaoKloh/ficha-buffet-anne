package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parcela do contrato de um evento. O lançamento espelho aponta para ela (lancamentos_financeiros.parcela_id). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "parcelas", uniqueConstraints = @UniqueConstraint(name = "uk_parcelas_evento_numero", columnNames = { "evento_id", "numero" }))
public class ParcelaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "vencimento", nullable = false)
    private LocalDate vencimento;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "pago_em")
    private LocalDate pagoEm;

    // pix, transferencia, boleto, cartao, dinheiro ou vazio.
    @Column(name = "forma", nullable = false, length = 15)
    private String forma = "";
}
