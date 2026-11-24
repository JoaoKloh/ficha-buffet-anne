package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
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

/** Movimento de estoque. O saldo de um item é a soma dos seus movimentos. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "movimentos_estoque")
public class MovimentoEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // inventario, entrada, sobra, perda, devolucao, ajuste ou consumo.
    @Column(name = "tipo", nullable = false, length = 15)
    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private IngredientesEntity ingrediente;

    // Efeito no saldo, com sinal.
    @Column(name = "qtd", nullable = false)
    private Double qtd;

    // Quantidade digitada (em ajuste e inventário, a contagem).
    @Column(name = "qtd_informada", nullable = false)
    private Double qtdInformada;

    @Column(name = "unidade", nullable = false, length = 10)
    private String unidade;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoEntity evento;

    @Column(name = "custo_unit", precision = 15, scale = 4)
    private BigDecimal custoUnit;

    // manual, inventario, nf, checklist ou consumo.
    @Column(name = "origem", nullable = false, length = 12)
    private String origem = "manual";

    // Entrada: validade do que entrou (perecíveis).
    @Column(name = "validade")
    private LocalDate validade;

    // Consumo: unidade de negócio que usou o item (buffet, restaurante ou chocolates).
    @Column(name = "unidade_uso", length = 12)
    private String unidadeUso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id")
    private NotaFiscalEntity notaFiscal;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();
}
