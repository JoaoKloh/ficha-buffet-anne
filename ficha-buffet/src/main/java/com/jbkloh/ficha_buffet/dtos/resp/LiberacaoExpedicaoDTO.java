package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;
import java.util.Map;

/**
 * Liberação do evento para expedição: conferências, horário de saída e autorização, com o
 * que ainda falta (ordens abertas, itens do checklist, conferências, horário).
 * situacao = [tom, rótulo].
 */
public record LiberacaoExpedicaoDTO(Long eventoId, Map<String, Boolean> ck, String horaSaida, String autorizadoPor, String autorizadoEm, boolean comPendencias,
        String pendencias, int ordensProducao, int ordensAbertas, int checklistProntos, int checklistTotal, boolean ok, List<String> faltam,
        List<String> situacao) {}
