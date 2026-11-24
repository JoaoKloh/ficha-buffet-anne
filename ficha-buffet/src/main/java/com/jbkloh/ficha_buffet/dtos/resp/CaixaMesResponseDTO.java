package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Aba Lançamentos do Caixa: KPIs do mês por competência, lista filtrada, categorias e eventos. */
public record CaixaMesResponseDTO(String mes, String unidade, Kpis kpis, List<LancamentoListaItemDTO> lancamentos, List<CategoriaComTotal> categorias,
        List<EventoRef> eventos, int semUnidade) {

    /** aReceber e aPagar: saldo em aberto dos lançamentos do mês. */
    public record Kpis(double receitas, double despesas, double aReceber, double aPagar, double resultado, Double margem, int totalLancamentos) {}

    public record CategoriaComTotal(@JsonUnwrapped CategoriaResponseDTO categoria, int totalLancamentos) {}

    public record EventoRef(Long id, String nome, String dataEvento) {}
}
