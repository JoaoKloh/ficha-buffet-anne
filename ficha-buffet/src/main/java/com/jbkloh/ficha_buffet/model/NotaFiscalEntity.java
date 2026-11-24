package com.jbkloh.ficha_buffet.model;

import com.jbkloh.ficha_buffet.model.json.ParcelaNf;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

/** Nota fiscal de compra: rascunho até dar entrada no estoque (e no caixa). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "notas_fiscais")
public class NotaFiscalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fornecedor", nullable = false, length = 120)
    private String fornecedor;

    @Column(name = "numero", nullable = false, length = 20)
    private String numero = "";

    @Column(name = "serie", nullable = false, length = 10)
    private String serie = "";

    @Column(name = "emissao", nullable = false)
    private LocalDate emissao;

    // Nulo: compra para o estoque da fábrica.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private EventoEntity evento;

    @Column(name = "lancar_caixa", nullable = false)
    private Boolean lancarCaixa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaFinanceiraEntity categoria;

    // boleto, pix, transferencia, cartao, dinheiro ou prazo.
    @Column(name = "forma_pag", nullable = false, length = 15)
    private String formaPag = "boleto";

    // Unidade de negócio que paga e usa a compra; vai para os lançamentos da nota.
    @Column(name = "unidade", length = 15)
    private String unidade;

    // Parcelas combinadas na nota; ao dar entrada viram lançamentos no caixa.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parcelas", nullable = false)
    private List<ParcelaNf> parcelas = new ArrayList<>();

    // rascunho ou lancada.
    @Column(name = "status", nullable = false, length = 10)
    private String status = "rascunho";

    // Frete, impostos ou desconto (negativo) fora dos itens.
    @Column(name = "acrescimo", nullable = false, precision = 15, scale = 2)
    private BigDecimal acrescimo = BigDecimal.ZERO;

    // Chave de acesso da NF-e (44 dígitos) ou vazio.
    @Column(name = "chave", nullable = false, length = 44)
    private String chave = "";

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial = "";

    @Column(name = "cnpj_fornecedor", nullable = false, length = 18)
    private String cnpjFornecedor = "";

    @Column(name = "importada_xml", nullable = false)
    private Boolean importadaXml = false;

    @Column(name = "importada_foto", nullable = false)
    private Boolean importadaFoto = false;

    @Column(name = "lancada_em")
    private LocalDate lancadaEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @OneToMany(mappedBy = "notaFiscal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<NotaFiscalItemEntity> itens = new ArrayList<>();
}
