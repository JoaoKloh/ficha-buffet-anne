package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Plano aberto: lista de produção, insumos por item e lista de compras (só dos itens ainda não concluídos). */
public record PlanoDetalheResponseDTO(PlanoProducaoResponseDTO plano, List<LinhaPlanoDTO> linhas, List<InsumosItemPlanoDTO> insumos, Compras compras) {

    public record Compras(Kpis kpis, List<CompraPlanoDTO> itens) {}

    /** custo: estimado do que falta comprar; semPreco: itens a comprar sem preço; concluidos: itens já feitos. */
    public record Kpis(int necessarios, int aComprar, double custo, int semPreco, int concluidos) {}
}
