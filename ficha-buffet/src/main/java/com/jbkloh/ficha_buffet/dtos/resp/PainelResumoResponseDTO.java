package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

/** Tudo que o Painel mostra: KPIs do mês, alertas, próximos eventos, resultado por unidade e séries dos gráficos. */
public record PainelResumoResponseDTO(String hoje, String mes, Kpis kpis, List<AlertaDTO> alertas, List<EventoListaItemDTO> proximos, Graficos graficos,
        Unidades unidades) {

    /**
     * faturamento: competência do mês com previstos; recebido: regime de caixa.
     * aReceber: saldo das receitas em aberto vencendo em até 30 dias; aPagar: das despesas em até 7 dias.
     */
    public record Kpis(double faturamento, double recebido, double aReceber, int contasReceber, int receberVencidas, double receberVencido, double aPagar,
            int contasPagar, int pagarVencidas, double pagarVencido, double resultado, Double margem) {}

    public record Funil(String status, String rotulo, int n) {}

    public record DespesaCategoria(String nome, double valor) {}

    public record Graficos(List<String> meses, int mesAtual, List<Double> receitas, List<Double> despesas, List<Double> resultado, List<String> mesesEventos,
            List<Integer> eventos, List<Integer> convidados, List<Funil> funil, int totalEventos, int contratadosOuAdiante,
            List<DespesaCategoria> despesasCategoria, String despesasDe) {}

    /** Resultado do mês por unidade operacional (competência, com previstos e estrutura rateada). */
    public record Unidades(List<ResultadoUnidade> linhas, int semUnidadeMes, Map<String, Double> rateio) {}

    public record ResultadoUnidade(String unidade, String rotulo, double faturamento, double cmv, Double pctCmv, double margemContribuicao, double resultado,
            Double margem) {}
}
