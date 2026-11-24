package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Prato da carta do restaurante. */
public record PratoVendaResponseDTO(Long id, String nome, String secao, double preco, String codigoPdv, boolean ativo, String obs,
        List<Componente> componentes, List<Extra> extras, String atualizadoEm) {

    public record Componente(Long preparacaoId, double qtd) {}

    public record Extra(Long insumoId, double qtd) {}
}
