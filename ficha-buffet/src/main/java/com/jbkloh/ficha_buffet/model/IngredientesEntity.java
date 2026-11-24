package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Item de estoque: ingrediente, embalagem, descartável, louça, utensílio ou produto de limpeza. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ingredientes")
public class IngredientesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    // ingrediente, embalagem, descartavel, louca, utensilio ou limpeza.
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "ingrediente";

    @Column(name = "categoria", nullable = false, length = 40)
    private String categoria = "Outros";

    // kg, l, un, dz, maço, garrafa ou botijão.
    @Column(name = "unidade", nullable = false, length = 10)
    private String unidade = "kg";

    @Column(name = "local", nullable = false, length = 40)
    private String local = "Depósito";

    @Column(name = "unidade_compra", nullable = false, length = 60)
    private String unidadeCompra = "";

    // Quantas unidades de estoque vêm em uma unidade de compra (ex.: caixa com 12).
    @Column(name = "fator_compra")
    private Double fatorCompra;

    @Column(name = "estoque_minimo", nullable = false)
    private Double estoqueMinimo = 0.0;

    // Custo por unidade de estoque, com 4 casas para não perder centavos em valor ÷ quantidade.
    @Column(name = "custo_por_unidade", nullable = false, precision = 15, scale = 4)
    private BigDecimal custo = BigDecimal.ZERO;

    @Column(name = "fornecedor")
    private String fornecedor;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDate atualizadoEm = LocalDate.now();
}
