package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.jbkloh.ficha_buffet.model.json.HistoricoOrdem;

/** O que a equipe registrou numa ordem de produção. Datas em ISO; motivo como aparece na tela. */
public record ExecucaoOrdemDTO(String iniciadoPor, String iniciadoEm, String concluidoPor, String concluidoEm, Double qtd, double perda, String obs,
        String conferidoPor, String conferidoEm, String lote, String motivo, String motivoTxt, List<HistoricoOrdem> hist) {

    public static final ExecucaoOrdemDTO VAZIA = new ExecucaoOrdemDTO("", null, "", null, null, 0, "", "", null, "", "", "", List.of());
}
