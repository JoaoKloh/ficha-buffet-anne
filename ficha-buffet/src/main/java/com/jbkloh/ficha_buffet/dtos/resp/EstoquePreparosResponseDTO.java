package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Pré-preparos: KPIs, pronto agora (usar primeiro o que vence antes) e lotes filtrados. */
public record EstoquePreparosResponseDTO(Kpis kpis, List<Pronto> prontoAgora, List<LoteListaItemDTO> lotes) {

    public record Kpis(int comSaldo, int preparacoes, int vencemHojeAmanha, int vencidos, double valor) {}

    public record Pronto(Long preparacaoId, String nome, double total, String unidade, int lotes, LoteListaItemDTO usarPrimeiro) {}
}
