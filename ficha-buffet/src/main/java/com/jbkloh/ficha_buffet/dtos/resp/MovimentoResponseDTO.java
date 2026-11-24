package com.jbkloh.ficha_buffet.dtos.resp;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Movimento de estoque. "virtual" marca a baixa calculada (checklist, ordem ou lote), que não é gravada.
 * validade: só em entrada (perecíveis). unidadeUso: só em consumo (unidade que usou o item).
 */
public record MovimentoResponseDTO(Long id, String tipo, Long insumoId, double qtd, double qtdInformada, String unidade, String data, String motivo,
        Long eventoId, Double custoUnit, String origem, Long nfId, String criadoEm, String validade, String unidadeUso,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean virtual) {}
