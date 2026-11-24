package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

/** Plano de produção do restaurante ou da chocolates, separado dos eventos do buffet. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "planos_producao")
public class PlanoProducaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 160)
    private String titulo;

    // restaurante ou chocolates (Dominio.RE_UNIDADE_PLANO).
    @Column(name = "unidade", nullable = false, length = 12)
    private String unidade;

    // planejado, em_producao ou concluido (Dominio.RE_STATUS_PLANO).
    @Column(name = "status", nullable = false, length = 12)
    private String status = "planejado";

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "obs", nullable = false, length = 2000)
    private String obs = "";

    // Pratos da carta previstos no período: pratoVendaId -> quantidade.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previsao", nullable = false)
    private Map<String, Integer> previsao = new LinkedHashMap<>();

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PlanoProducaoItemEntity> itens = new ArrayList<>();
}
