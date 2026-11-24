package com.jbkloh.ficha_buffet.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Item do plano de produção: a ficha e a quantidade na unidade de rendimento dela. Concluído,
 * aponta para o lote criado em Estoque › Pré-preparos (que baixa os insumos).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "planos_producao_itens")
public class PlanoProducaoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoProducaoEntity plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preparacao_id", nullable = false)
    private PreparacaoEntity preparacao;

    @Column(name = "qtd", nullable = false)
    private Double qtd;

    @Column(name = "feito", nullable = false)
    private boolean feito;

    @Column(name = "feito_em")
    private LocalDate feitoEm;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_preparo_id", unique = true)
    private LotePreparoEntity lote;
}
