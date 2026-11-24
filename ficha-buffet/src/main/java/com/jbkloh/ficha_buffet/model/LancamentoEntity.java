package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lancamentos_financeiros")
public class LancamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "receita" ou "despesa" (acompanha o grupo da categoria) ou "transferencia" entre unidades.
    @Column(name = "tipo", nullable = false, length = 15)
    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaFinanceiraEntity categoria;

    // Opcional: o evento a que a receita ou o custo pertence.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoEntity evento;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    // BigDecimal (e não Double) para não perder centavos em valores monetários.
    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "vencimento")
    private LocalDate vencimento;

    // previsto, realizado ou cancelado.
    @Column(name = "status", nullable = false, length = 10)
    private String status = "realizado";

    // manual, parcela, extra, nf, equipe ou transferencia.
    @Column(name = "origem", nullable = false, length = 15)
    private String origem = "manual";

    // banco (Buffet), banco_restaurante ou caixa.
    @Column(name = "conta", nullable = false, length = 20)
    private String conta = "banco";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_id")
    private ParcelaEntity parcela;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extra_id")
    private ExtraEventoEntity extra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id")
    private NotaFiscalEntity notaFiscal;

    // Número da parcela da nota fiscal que gerou o lançamento.
    @Column(name = "parcela_nf")
    private Integer parcelaNF;

    @Column(name = "forma_pag", length = 15)
    private String formaPag;

    @Column(name = "fornecedor", length = 120)
    private String fornecedor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    // Unidade de negócio: buffet, restaurante, chocolates ou compartilhado. Nula = ainda sem unidade.
    @Column(name = "unidade", length = 15)
    private String unidade;

    // Transferência entre unidades: o custo sai da origem e entra no destino, sem mexer no caixa.
    @Column(name = "unidade_origem", length = 15)
    private String unidadeOrigem;

    @Column(name = "unidade_destino", length = 15)
    private String unidadeDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id")
    private IngredientesEntity insumo;

    @Column(name = "item", length = 200)
    private String item;

    @Column(name = "qtd")
    private Double qtd;

    @Column(name = "unid", length = 20)
    private String unid;

    @Column(name = "responsavel", length = 100)
    private String responsavel;

    @Column(name = "obs", length = 500)
    private String obs;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @Column(name = "alterado_por", length = 100)
    private String alteradoPor;

    @Column(name = "alterado_em")
    private Instant alteradoEm;

    // Pagamentos ou recebimentos, do mais antigo para o mais recente. Realizado = soma >= valor.
    @OneToMany(mappedBy = "lancamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LancamentoPagamentoEntity> pagamentos = new ArrayList<>();
}
