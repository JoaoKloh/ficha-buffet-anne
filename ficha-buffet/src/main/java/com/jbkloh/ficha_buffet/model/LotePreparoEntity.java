package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lote de pré-preparo produzido pela cozinha, com validade e saldo. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lotes_preparo")
public class LotePreparoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "LAAMMDD-NN".
    @Column(name = "codigo", nullable = false, unique = true, length = 12)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preparacao_id", nullable = false)
    private PreparacaoEntity preparacao;

    @Column(name = "qtd", nullable = false)
    private Double qtd;

    // Quantidade produzida menos usos e descartes; mantido junto com cada movimento do lote.
    @Column(name = "saldo", nullable = false)
    private Double saldo;

    @Column(name = "unidade", nullable = false, length = 10)
    private String unidade;

    @Column(name = "produzido_em", nullable = false)
    private LocalDate produzidoEm;

    @Column(name = "validade")
    private LocalDate validade;

    // restaurante, geral ou evento (com evento obrigatório).
    @Column(name = "destino", nullable = false, length = 15)
    private String destino = "restaurante";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoEntity evento;

    @Column(name = "responsavel", nullable = false, length = 100)
    private String responsavel = "";

    @Column(name = "obs", nullable = false, length = 1000)
    private String obs = "";

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    // restaurante ou chocolates: só no lote de item de plano de produção; nulo conta como restaurante.
    @Column(name = "unidade_negocio", length = 12)
    private String unidadeNegocio;

    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("em")
    private List<LoteMovimentoEntity> historico = new ArrayList<>();
}
