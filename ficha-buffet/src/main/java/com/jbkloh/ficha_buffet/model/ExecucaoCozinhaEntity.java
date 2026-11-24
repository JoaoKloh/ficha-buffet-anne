package com.jbkloh.ficha_buffet.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

/**
 * O que a cozinha registra de um evento: tarefa feita, compras marcadas, linha
 * do checklist de envio, louça que saiu e voltou, assinaturas do transporte.
 * Um registro por evento + tipo + referência; os campos usados dependem do tipo.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "cozinha_execucoes")
public class ExecucaoCozinhaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    // item, comp, extra, louca, tarefa, compras ou transporte.
    @Column(name = "kind", nullable = false, length = 12)
    private String kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_cardapio_id")
    private ItemCardapioEntity itemCardapio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id")
    private IngredientesEntity ingrediente;

    // Chave da linha dentro do tipo (componente, extra ou tarefa).
    @Column(name = "referencia", length = 60)
    private String referencia;

    // A quantidade produzida do prato dá baixa dos ingredientes no estoque.
    @Column(name = "produzido")
    private Double produzido;

    @Column(name = "produzido_em")
    private LocalDate produzidoEm;

    @Column(name = "executado_por", nullable = false, length = 100)
    private String executadoPor = "";

    @Column(name = "enviado_q")
    private Double enviadoQ;

    @Column(name = "enviado", nullable = false)
    private Boolean enviado = false;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(name = "separado_por", nullable = false, length = 100)
    private String separadoPor = "";

    @Column(name = "retorno")
    private Double retorno;

    @Column(name = "conferido_por", nullable = false, length = 100)
    private String conferidoPor = "";

    @Column(name = "obs", nullable = false, length = 300)
    private String obs = "";

    @Column(name = "temp_saida")
    private Double tempSaida;

    @Column(name = "temp_chegada")
    private Double tempChegada;

    // Louças: quantas saíram, voltaram e foram lançadas como avaria.
    @Column(name = "saiu")
    private Double saiu;

    @Column(name = "voltou")
    private Double voltou;

    @Column(name = "avaria")
    private Double avaria;

    // Tarefas: feita, responsável e dia movido (D-n).
    @Column(name = "feito", nullable = false)
    private Boolean feito = false;

    @Column(name = "resp", nullable = false, length = 100)
    private String resp = "";

    @Column(name = "dn_set", nullable = false)
    private Boolean dnSet = false;

    @Column(name = "dn")
    private Integer dn;

    // Compras: id do item -> comprado.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "itens_comprados", nullable = false)
    private Map<String, Boolean> itensComprados = new HashMap<>();

    // Transporte: quem assinou cada etapa e a que horas.
    @Column(name = "chef_por", nullable = false, length = 100)
    private String chefPor = "";

    @Column(name = "chef_hora")
    private LocalTime chefHora;

    @Column(name = "carga_por", nullable = false, length = 100)
    private String cargaPor = "";

    @Column(name = "carga_hora")
    private LocalTime cargaHora;

    @Column(name = "receb_por", nullable = false, length = 100)
    private String recebPor = "";

    @Column(name = "receb_hora")
    private LocalTime recebHora;

    @Column(name = "retorno_por", nullable = false, length = 100)
    private String retornoPor = "";

    @Column(name = "retorno_hora")
    private LocalTime retornoHora;

    @Column(name = "veiculo", nullable = false, length = 100)
    private String veiculo = "";

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();
}
