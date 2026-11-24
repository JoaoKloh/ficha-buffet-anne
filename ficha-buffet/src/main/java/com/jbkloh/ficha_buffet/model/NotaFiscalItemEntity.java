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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "nota_fiscal_item")
public class NotaFiscalItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id", nullable = false)
    private NotaFiscalEntity notaFiscal;

    @Column(name = "ordem", nullable = false)
    private Integer ordem = 0;

    // Descrição, código e unidade como vieram na nota.
    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao = "";

    @Column(name = "codigo", nullable = false, length = 60)
    private String codigo = "";

    @Column(name = "unidade_comercial", nullable = false, length = 10)
    private String unidadeComercial = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id")
    private IngredientesEntity ingrediente;

    @Column(name = "qtd_nota", nullable = false)
    private Double qtdNota = 0.0;

    // "estoque" ou "compra": em que unidade a quantidade da nota está.
    @Column(name = "unid", nullable = false, length = 8)
    private String unid = "estoque";

    // Quantidade já na unidade de estoque (guardada: o fator de compra do item pode mudar depois).
    @Column(name = "qtd", nullable = false)
    private Double qtd = 0.0;

    @Column(name = "valor_unit", precision = 15, scale = 4)
    private BigDecimal valorUnit;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    @Column(name = "conferido", nullable = false)
    private Boolean conferido = false;
}
