package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contrato do evento: vive nas colunas contrato_* de "producao", todas nulas enquanto não há contrato. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ContratoEmbeddable {

    @Column(name = "contrato_numero", length = 40)
    private String numero;

    @Column(name = "contrato_data_assinatura")
    private LocalDate dataAssinatura;

    @Column(name = "contrato_convidados")
    private Integer convidadosContratados;

    @Column(name = "contrato_valor_por_convidado", precision = 15, scale = 2)
    private BigDecimal valorPorConvidado;

    @Column(name = "contrato_valor_total", precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "contrato_valor_convidado_extra", precision = 15, scale = 2)
    private BigDecimal valorConvidadoExtra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_categoria_id")
    private CategoriaFinanceiraEntity categoria;
}
