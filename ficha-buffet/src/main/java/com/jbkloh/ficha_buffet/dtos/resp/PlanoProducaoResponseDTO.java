package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

/** Plano de produção do restaurante ou da chocolates. previsao = pratos da carta previstos (pratoVendaId -> quantidade). */
public record PlanoProducaoResponseDTO(Long id, String titulo, String unidade, String status, String dataInicio, String dataFim, String obs,
        List<ItemPlanoDTO> itens, Map<String, Integer> previsao, String criadoEm, String atualizadoEm) {}
