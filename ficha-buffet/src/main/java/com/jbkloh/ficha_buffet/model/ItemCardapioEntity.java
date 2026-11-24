package com.jbkloh.ficha_buffet.model;

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

/** Prato do cardápio de um evento. A tabela continua "producao_prato". */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "producao_prato")
public class ItemCardapioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preparacao_id", nullable = false)
    private PreparacaoEntity preparacao;

    // Porção por convidado, na unidade de rendimento da ficha.
    @Column(name = "porcao", nullable = false)
    private Double porcao;

    // sim, definir ou nao.
    @Column(name = "status", nullable = false, length = 10)
    private String status = "sim";

    // Nula: vale a praça da ficha.
    @Column(name = "praca", length = 20)
    private String praca;

    @Column(name = "responsavel", nullable = false, length = 100)
    private String responsavel = "";
}
