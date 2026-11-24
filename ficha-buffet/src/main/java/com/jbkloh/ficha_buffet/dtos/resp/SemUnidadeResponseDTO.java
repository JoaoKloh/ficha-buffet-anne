package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Lançamentos ainda sem unidade de negócio (até 300, os mais recentes primeiro) e quantos são ao todo. */
public record SemUnidadeResponseDTO(int total, List<LancamentoListaItemDTO> lancamentos) {}
