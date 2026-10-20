package com.jbkloh.ficha_buffet.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
@Table(name = "orcamentos")
@AllArgsConstructor 
@NoArgsConstructor
public class OrcamentoEntity {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_cliente", nullable = false)
    private String nomeCliente;

    @Column(name = "contato_cliente")
    private String contatoCliente;

    @Column(name = "tipo_servico")
    private String tipoServico;

    // Em horas, como o campo "Duração" da aba Evento do frontend.
    @Column(name = "duracao_evento", nullable = false)
    private Double duracaoEvento;

    @Column(name = "numero_convidados", nullable = false)
    private Integer numeroConvidados;

    // BigDecimal (e não Double) para não perder centavos em valores monetários.
    @Column(name = "valor_alimentos", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorAlimentos;

    @Column(name = "valor_equipe", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorEquipe;

    @Column(name = "valor_degustacao", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorDegustacao;

    @Column(name = "valor_outros", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorOutros;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_por_pessoa", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPorPessoa;
}
