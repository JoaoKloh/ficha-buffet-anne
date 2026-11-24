package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Transferências entre unidades: o saldo de cada unidade no mês e a lista, da mais recente para a mais antiga. */
public record TransferenciasResponseDTO(String unidade, String mes, List<PorUnidade> porUnidade, int total, int doMes, List<LancamentoListaItemDTO> transferencias,
        int semUnidade) {

    public record PorUnidade(String unidade, double recebeu, double enviou, double liquido) {}
}
