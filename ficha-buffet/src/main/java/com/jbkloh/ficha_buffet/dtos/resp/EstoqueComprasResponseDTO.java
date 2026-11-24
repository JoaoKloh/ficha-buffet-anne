package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Sugestão de compras do horizonte, agrupada por fornecedor ("Sem fornecedor definido" por último). */
public record EstoqueComprasResponseDTO(int horizonte, Kpis kpis, List<Fornecedor> fornecedores) {

    /** itens, fornecedores, valor e semPreco contam só o que ainda não foi pedido. */
    public record Kpis(int itens, int fornecedores, double valor, boolean semPreco, int pedidos, int eventos) {}

    /** valor: soma dos itens ainda não pedidos. */
    public record Fornecedor(String fornecedor, double valor, List<SugestaoCompraDTO> itens) {}
}
