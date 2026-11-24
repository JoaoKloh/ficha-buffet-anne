package com.jbkloh.ficha_buffet.dtos.resp;

/**
 * Linha da lista de produção. nome nulo = ficha removida; unidade já no rótulo certo ("porções",
 * "un", "kg"); receitas = quantidade / rendimento da ficha; lote = o lote criado ao concluir.
 */
public record LinhaPlanoDTO(String k, Long preparacaoId, String nome, String categoria, String categoriaLabel, String praca, double qtd, String unidade,
        double receitas, double rendimento, String fichaStatus, Integer validadeDias, boolean feito, String feitoEm, Lote lote) {

    public record Lote(Long id, String codigo) {}
}
