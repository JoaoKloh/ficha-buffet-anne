package com.jbkloh.ficha_buffet.model;

import com.jbkloh.ficha_buffet.model.json.EnvioExtra;
import com.jbkloh.ficha_buffet.model.json.PlanoCozinhaItem;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

/** Evento do buffet, do lead ao acerto. A tabela continua "producao". */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "producao")
public class EventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "AA-NNN": ano do evento e sequência dentro do ano. Gerado uma vez.
    @Column(name = "codigo", nullable = false, unique = true, length = 6)
    private String codigo;

    @Column(name = "nome", nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    // casamento, 15_anos, corporativo, coffee_break ou outro.
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "outro";

    // lead, orcamento_enviado, degustacao, contratado, cardapio_fechado, em_producao,
    // realizado, acertado, perdido ou cancelado.
    @Column(name = "status", nullable = false, length = 20)
    private String status = "lead";

    // Nula enquanto a data está a combinar.
    @Column(name = "data_evento")
    private LocalDate dataEvento;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "local", nullable = false, length = 200)
    private String local = "";

    @Column(name = "cidade", nullable = false, length = 100)
    private String cidade = "";

    @Column(name = "quantidade_convidados")
    private Integer convidadosPrevistos;

    // Margem de produção, em % sobre os convidados.
    @Column(name = "margem", nullable = false)
    private Double margem = 0.0;

    @Column(name = "observacoes", nullable = false, length = 4000)
    private String observacoes = "";

    @Embedded
    private ContratoEmbeddable contrato;

    @Column(name = "cardapio_fechado_em")
    private LocalDate cardapioFechadoEm;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemCardapioEntity> cardapio = new ArrayList<>();

    // Itens de serviço, prataria e descartáveis do checklist de envio.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "envio_extras", nullable = false)
    private List<EnvioExtra> envioExtras = new ArrayList<>();

    // Plano de tarefas montado pela gestão; nulo usa as tarefas fixas.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plano_cozinha")
    private List<PlanoCozinhaItem> planoCozinha;

    @Column(name = "equipe_cozinha", nullable = false, length = 200)
    private String equipeCozinha = "";

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    /** Põe a ficha no cardápio com a porção padrão dela, confirmada ("sim"). */
    public ItemCardapioEntity addPreparacao(PreparacaoEntity preparacao) {
        ItemCardapioEntity item = new ItemCardapioEntity();
        item.setEvento(this);
        item.setPreparacao(preparacao);
        Double porcao = preparacao.getPorcaoPadrao();
        item.setPorcao(porcao != null && porcao > 0 ? porcao : 1.0);
        this.cardapio.add(item);
        return item;
    }
}
