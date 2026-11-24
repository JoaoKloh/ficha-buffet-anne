package com.jbkloh.ficha_buffet.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.jbkloh.ficha_buffet.model.json.ComponenteEnvio;
import com.jbkloh.ficha_buffet.model.json.EtapaPreparacao;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

/** Ficha técnica (preparação). A tabela continua "pratos". */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "pratos")
public class PreparacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    // canape, volante, entrada, ilha, principal, acompanhamento, sobremesa, bebida ou base.
    @Column(name = "categoria", nullable = false, length = 20)
    private String categoria;

    @Column(name = "praca", nullable = false, length = 20)
    private String praca = "Cozinha quente";

    // refrigerado, congelado, quente ou ambiente.
    @Column(name = "conservacao", nullable = false, length = 15)
    private String conservacao = "refrigerado";

    // Nulo: vale a validade padrão da conservação.
    @Column(name = "validade_dias")
    private Integer validadeDias;

    /* Campos de produção, usados nas ordens de produção da cozinha. */

    // Id do setor da cozinha; "" = automático pela praça e categoria.
    @Column(name = "setor", nullable = false, length = 40)
    private String setor = "";

    // Dias antes do evento; nulo = padrão da praça.
    @Column(name = "dn_producao")
    private Integer dnProducao;

    // Minutos por receita; nulo = não informado.
    @Column(name = "tempo_min")
    private Integer tempoMin;

    @Column(name = "equipamentos", nullable = false, length = 200)
    private String equipamentos = "";

    @Column(name = "armazenamento", nullable = false, length = 300)
    private String armazenamento = "";

    // O fornecedor entrega pronto: a produção só retira do estoque.
    @Column(name = "comprado_pronto", nullable = false)
    private boolean compradoPronto;

    @Column(name = "rendimento_da_receita", nullable = false)
    private Double rendimento;

    // porções, un, kg ou l.
    @Column(name = "unidade", nullable = false, length = 10)
    private String unidadeRendimento = "porções";

    // Porção sugerida por convidado, na unidade do rendimento.
    @Column(name = "rendimento_por_pessoa", nullable = false)
    private Double porcaoPadrao = 1.0;

    @Column(name = "descricao", nullable = false, length = 4000)
    private String modoPreparo = "";

    @Column(name = "obs_envio", nullable = false, length = 300)
    private String obsEnvio = "";

    @Column(name = "img_url")
    private String urlImagem;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDate atualizadoEm = LocalDate.now();

    // cascade/orphanRemoval alcançam só a linha de prato_ingrediente (a
    // associação), nunca a IngredientesEntity referenciada por ela — apagar
    // uma ficha não pode apagar item do cadastro.
    @OneToMany(mappedBy = "preparacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    @JsonManagedReference
    private List<PratoIngredienteEntity> itensIngredientes = new ArrayList<>();

    @OneToMany(mappedBy = "preparacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem")
    private List<PratoLoucaEntity> loucas = new ArrayList<>();

    // Etapas e componentes de envio não referenciam outras tabelas e são sempre
    // lidos com a ficha: ficam em JSON.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "etapas", nullable = false)
    private List<EtapaPreparacao> etapas = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "componentes", nullable = false)
    private List<ComponenteEnvio> componentes = new ArrayList<>();

    public void addIngrediente(IngredientesEntity ingrediente, Double quantidade) {
        PratoIngredienteEntity item = new PratoIngredienteEntity();
        item.setPreparacao(this);
        item.setIngrediente(ingrediente);
        item.setQuantidade(quantidade);
        item.setOrdem(this.itensIngredientes.size());
        this.itensIngredientes.add(item);
    }
}
