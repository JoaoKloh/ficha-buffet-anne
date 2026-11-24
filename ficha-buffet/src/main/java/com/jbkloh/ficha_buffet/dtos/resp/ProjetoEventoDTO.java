package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/**
 * Evento como projeto: plano de logística, orçamento, assinaturas que a cozinha deixou no
 * checklist de transporte (carga, recebimento no evento e retorno; nulas enquanto pendentes),
 * retrato do fechamento (nulo se o evento nunca foi fechado) e o que conferir antes de fechar
 * (vazio = tudo conferido).
 */
public record ProjetoEventoDTO(LogisticaEventoDTO logistica, OrcamentoEventoDTO orcamento, Assinaturas assinaturas, FechamentoEventoDTO fechamento,
        List<String> pendenciasFechamento) {

    public record Assinaturas(Assinatura carga, Assinatura recebimento, Assinatura retorno) {}

    public record Assinatura(String por, String hora) {}
}
