package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

/** Opções dos formulários de ficha e de prato de venda. setores = setores ativos da cozinha, [id, nome]. */
public record FichasOpcoesResponseDTO(List<GrupoOpcoesDTO> insumos, List<List<Object>> reutilizaveis, List<Preparacao> preparacoes,
        Map<Long, String> unidades, List<GrupoOpcoesDTO> preparacoesAgrupadas, List<List<Object>> setores) {

    public record Preparacao(Long id, String nome, String categoria, String unidade, double porcaoPadrao) {}
}
