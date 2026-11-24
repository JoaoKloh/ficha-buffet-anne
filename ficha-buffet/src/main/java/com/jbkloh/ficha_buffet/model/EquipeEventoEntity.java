package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pessoa da equipe escalada num evento: função, horários, diária e a conta a pagar da diária, quando lançada. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "evento_equipe", uniqueConstraints = @UniqueConstraint(name = "uq_evento_equipe_evento_pessoa", columnNames = { "evento_id", "equipe_id" }))
public class EquipeEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id", nullable = false)
    private EquipeEntity pessoa;

    // Uma das funções de Dominio.RE_FUNCAO_EQUIPE.
    @Column(name = "funcao", nullable = false, length = 30)
    private String funcao;

    @Column(name = "chegada")
    private LocalTime chegada;

    @Column(name = "saida")
    private LocalTime saida;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    // Conta a pagar da diária; nula enquanto a diária está fora do caixa. Excluída no Caixa, a diária volta a ficar fora dele.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lancamento_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private LancamentoEntity lancamento;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "criado_por", length = 100)
    private String criadoPor;
}
