package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Insumos de um item do plano: por receita (qb) e total do item (qt), com as sub-receitas abertas (via). */
public record InsumosItemPlanoDTO(String k, String nome, String categoriaLabel, String praca, double qtd, String unidade, String rendimentoLabel,
        boolean feito, String modoPreparo, List<Insumo> ins) {

    public record Insumo(String n, String u, String via, double qb, double qt) {}
}
