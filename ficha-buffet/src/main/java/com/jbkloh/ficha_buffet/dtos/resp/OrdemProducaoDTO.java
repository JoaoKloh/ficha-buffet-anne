package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Ordem de produção gerada do cardápio confirmado e das fichas: principal (m_), sub-receita (s_),
 * finalização (f_) e expedição (x_exp). data = dia em que tem de estar pronta (evento - dn).
 */
public record OrdemProducaoDTO(String k, Long eventoId, String codigo, String eventoNome, String eventoData, Long cid, Long prepId, String nome, String etapa,
        String setor, double q, String u, double rend, String rendU, double receitas, int dn, boolean dnPadrao, String data, String hora, String resp,
        List<String> origem, int tempoMin, String equip, String armaz, int valDias, List<Insumo> ins, List<String> passos, List<Componente> comp,
        String status, boolean atrasada, ExecucaoOrdemDTO exec, String etiqueta) {

    /** Ingrediente da ordem; sub = sub-receita, que tem ordem própria. */
    public record Insumo(String n, double q, String u, @JsonInclude(JsonInclude.Include.NON_NULL) Boolean sub) {}

    /** Componente que viaja separado. */
    public record Componente(String n, double q, String u) {}
}
