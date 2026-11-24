package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pessoa da equipe do buffet, cadastrada uma vez e escalada em quantos eventos for preciso.
 * Função e diária padrão servem à escala em lote; em cada evento a escala guarda as suas.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "equipe")
public class EquipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "contato", nullable = false, length = 40)
    private String contato = "";

    @Column(name = "pix", nullable = false, length = 120)
    private String pix = "";

    // Uma das funções de Dominio.RE_FUNCAO_EQUIPE.
    @Column(name = "funcao_padrao", nullable = false, length = 30)
    private String funcaoPadrao = "Garçom";

    @Column(name = "diaria_padrao", nullable = false, precision = 15, scale = 2)
    private BigDecimal diariaPadrao = BigDecimal.ZERO;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "criado_por", length = 100)
    private String criadoPor;
}
