package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@Table(name = "lancamentos_financeiros")
@AllArgsConstructor 
@NoArgsConstructor
public class LancamentoEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "receita" ou "despesa", exatamente como o frontend envia (TipoLancamento).
    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    // BigDecimal (e não Double) para não perder centavos em valores monetários.
    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_inicial_lancamento", nullable = false)
    private LocalDate dataInical;

    @Column(name = "data_final_lancamento",nullable = false)
    private LocalDate dataFinal;

    // Opcional: um lançamento pode não ter produção, ou ter várias; e uma produção
    // pode aparecer em vários lançamentos. Sem cascade: apagar um lançamento limpa
    // só as linhas de lancamento_producao, nunca as produções.
    @ManyToMany
    @JoinTable(
        name = "lancamento_producao",
        joinColumns = @JoinColumn(name = "lancamento_id"),
        inverseJoinColumns = @JoinColumn(name = "producao_id")
    )
    private List<ProducaoEntity> producoes = new ArrayList<>();
}
