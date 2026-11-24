package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cardápio do evento com quantidades, custo e situação das fichas. */
public record CardapioEventoDTO(@JsonProperty("N") int n, int base, double margem, boolean fechado, List<Linha> linhas, double custo, double custoDef,
        Double custoPorPessoa, Double pctContrato, double abLancado, int nDefinir, List<String> semFicha, boolean faltandoPreco) {

    public record Linha(ItemCardapioResponseDTO item, String preparacaoNome, String categoria, String categoriaLabel, String pracaEfetiva, String status,
            double total, String aProduzir, String fatorUnid, String fichaStatus, double custo, double custoDef, double porcaoPadrao, String rendimentoLabel) {}
}
