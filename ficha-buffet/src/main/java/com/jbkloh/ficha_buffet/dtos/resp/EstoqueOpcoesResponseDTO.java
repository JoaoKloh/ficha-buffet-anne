package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Opções dos formulários de estoque, lote, nota fiscal e transferência entre unidades. Pares são [id, rótulo]. Cada
 * formulário recebe só os campos que usa; os demais ficam nulos e não entram no JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstoqueOpcoesResponseDTO(List<GrupoOpcoesDTO> itens, Map<Long, ItemInfo> itensInfo, List<List<Object>> eventos,
        List<List<Object>> eventosAtivos, List<List<Object>> eventosLote, List<GrupoOpcoesDTO> preparacoes, Map<Long, PreparacaoInfo> preparacoesInfo,
        List<GrupoOpcoesDTO> categoriasDespesa, @JsonProperty("categoriaAB") Long categoriaAB, List<String> fornecedores) {

    /** custo: último preço pago por unidade de estoque. */
    public record ItemInfo(String nome, String unidade, Double fatorCompra, String unidadeCompra, boolean reutilizavel, double saldo, String local,
            double custo) {}

    public record PreparacaoInfo(String nome, String unidade, double rendimento, int validadeDias, String conservacao) {}
}
