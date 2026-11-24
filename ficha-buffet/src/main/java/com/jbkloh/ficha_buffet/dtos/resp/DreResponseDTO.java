package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

/**
 * DRE pronta para a tela, de uma unidade, do consolidado ou comparando as unidades
 * (`comparativo` preenchido e `linhas` vazias). regime: competencia ou caixa.
 */
public record DreResponseDTO(String ano, String periodo, boolean incluirPrevistos, String titulo, List<String> anos, List<Linha> linhas,
        List<ResultadoEvento> resultadoPorEvento, String unidade, String regime, boolean ratear, String descricaoRegime, Map<String, Double> rateio,
        boolean rateioDefinido, int semUnidade, Comparativo comparativo, List<String> categoriasInvestimento, double resultado) {

    /** tipo: sum (subtotal), grupo (dedução), cat (categoria), final ou fora (investimentos, fora do resultado). pct é fração da receita bruta. */
    public record Linha(String tipo, String rotulo, double valor, Double pct) {}

    public record ResultadoEvento(Long id, String nome, String dataEvento, double rec, double desp, double res, Double mg) {}

    /** Uma coluna por unidade (e o consolidado); cada linha tem um valor por coluna. */
    public record Comparativo(List<Coluna> colunas, List<LinhaComparativa> linhas, List<Double> margens, List<Double> investimentos) {}

    public record Coluna(String chave, String rotulo) {}

    public record LinhaComparativa(String tipo, String rotulo, List<Double> valores) {}
}
