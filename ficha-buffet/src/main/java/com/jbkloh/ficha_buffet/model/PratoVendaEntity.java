package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Prato da carta do restaurante, montado com fichas técnicas e itens diretos. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "pratos_venda")
public class PratoVendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    // serra, mar, campo, executivo, cafe, sobremesa, bebida ou outro.
    @Column(name = "secao", nullable = false, length = 15)
    private String secao = "campo";

    @Column(name = "preco", nullable = false, precision = 15, scale = 2)
    private BigDecimal preco = BigDecimal.ZERO;

    // Único quando preenchido.
    @Column(name = "codigo_pdv", nullable = false, length = 40)
    private String codigoPdv = "";

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "obs", nullable = false, length = 2000)
    private String obs = "";

    @Column(name = "atualizado_em", nullable = false)
    private LocalDate atualizadoEm = LocalDate.now();

    @OneToMany(mappedBy = "pratoVenda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<PratoVendaComponenteEntity> componentes = new ArrayList<>();

    @OneToMany(mappedBy = "pratoVenda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<PratoVendaInsumoEntity> insumos = new ArrayList<>();
}
