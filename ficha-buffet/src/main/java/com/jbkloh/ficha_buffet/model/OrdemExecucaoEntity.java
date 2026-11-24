package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.jbkloh.ficha_buffet.model.json.HistoricoOrdem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * O que a equipe registrou numa ordem de produção (iniciar, concluir, conferir, bloquear...).
 * Sem linha, a ordem está pendente. A baixa dos insumos sai das ordens concluídas.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_ordem_execucao",
    uniqueConstraints = @UniqueConstraint(name = "uq_evento_ordem_execucao_evento_chave", columnNames = { "evento_id", "chave" }))
public class OrdemExecucaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    @Column(name = "chave", nullable = false, length = 40)
    private String chave;

    // pendente, em_producao, em_conferencia, concluida ou bloqueada.
    @Column(name = "status", nullable = false, length = 16)
    private String status = "pendente";

    // Para onde a ordem volta ao ser desbloqueada.
    @Column(name = "status_anterior", length = 16)
    private String statusAnterior;

    @Column(name = "responsavel", nullable = false, length = 80)
    private String responsavel = "";

    @Column(name = "iniciado_por", nullable = false, length = 80)
    private String iniciadoPor = "";

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "concluido_por", nullable = false, length = 80)
    private String concluidoPor = "";

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    // Quantidade produzida, na unidade da ordem.
    @Column(name = "qtd")
    private Double qtd;

    @Column(name = "perda", nullable = false)
    private Double perda = 0.0;

    @Column(name = "obs", nullable = false, length = 2000)
    private String obs = "";

    @Column(name = "conferido_por", nullable = false, length = 80)
    private String conferidoPor = "";

    @Column(name = "conferido_em")
    private Instant conferidoEm;

    @Column(name = "lote", nullable = false, length = 40)
    private String lote = "";

    // Motivo do bloqueio, como aparece na tela ("Falta de insumo"...).
    @Column(name = "motivo", nullable = false, length = 40)
    private String motivo = "";

    @Column(name = "motivo_txt", nullable = false, length = 500)
    private String motivoTxt = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "historico", nullable = false)
    private List<HistoricoOrdem> historico = new ArrayList<>();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();
}
