package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Tela Produção de uma unidade: KPIs e os planos, abertos primeiro e depois os mais recentes. */
public record PlanosProducaoResponseDTO(String unidade, Kpis kpis, List<PlanoListaItemDTO> planos) {

    /** prePreparosProntos: lotes com saldo da unidade (lote sem unidade conta como restaurante). */
    public record Kpis(int abertos, int itensAProduzir, int concluidos, int prePreparosProntos) {}
}
