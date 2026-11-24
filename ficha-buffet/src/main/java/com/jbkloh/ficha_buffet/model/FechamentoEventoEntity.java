package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.jbkloh.ficha_buffet.model.json.CustosPorTipo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Retrato do resultado do evento guardado ao fechá-lo (passar para Acertado). Fechar de novo substitui o retrato. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_fechamento")
public class FechamentoEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    private EventoEntity evento;

    @Column(name = "fechado_em", nullable = false)
    private Instant fechadoEm = Instant.now();

    @Column(name = "fechado_por", nullable = false, length = 100)
    private String fechadoPor = "";

    @Column(name = "receita", nullable = false, precision = 15, scale = 2)
    private BigDecimal receita = BigDecimal.ZERO;

    @Column(name = "recebido", nullable = false, precision = 15, scale = 2)
    private BigDecimal recebido = BigDecimal.ZERO;

    @Column(name = "custo_previsto", nullable = false, precision = 15, scale = 2)
    private BigDecimal custoPrevisto = BigDecimal.ZERO;

    @Column(name = "custo_realizado", nullable = false, precision = 15, scale = 2)
    private BigDecimal custoRealizado = BigDecimal.ZERO;

    @Column(name = "resultado", nullable = false, precision = 15, scale = 2)
    private BigDecimal resultado = BigDecimal.ZERO;

    // A&B realizado sobre a receita, com 3 casas; nulo sem receita.
    @Column(name = "cmv", precision = 9, scale = 3)
    private BigDecimal cmv;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "por_tipo", nullable = false)
    private CustosPorTipo porTipo;

    // Etapas (Dominio.ETAPAS_EVENTO) ainda sem marcar quando o evento foi fechado.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pendencias", nullable = false)
    private List<String> pendencias = new ArrayList<>();
}
