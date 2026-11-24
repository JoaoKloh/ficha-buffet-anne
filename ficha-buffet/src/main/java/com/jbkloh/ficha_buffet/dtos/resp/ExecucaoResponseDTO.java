package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.Map;

/**
 * Registro de execução da cozinha. A chave é evento + tipo + referência:
 * "i_12", "c_12__c0", "x_3__xabc", "l_3__7", "t_3__f_0", "cp_3", "tr_3".
 */
public record ExecucaoResponseDTO(Long id, String chave, String kind, Long eventoId, Long cardapioId, Long insumoId, String key, Double produzido,
        String produzidoEm, String executadoPor, Double enviadoQ, boolean enviado, String enviadoEm, String separadoPor, Double retorno,
        String conferidoPor, String obs, Double tempSaida, Double tempChegada, Double saiu, Double voltou, Double avaria, boolean feito, String resp,
        boolean dnSet, Integer dn, Map<String, Boolean> itens, String chefPor, String chefHora, String cargaPor, String cargaHora, String recebPor,
        String recebHora, String retornoPor, String retornoHora, String veiculo, String atualizadoEm) {}
