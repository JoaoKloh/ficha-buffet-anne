package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Nota com o evento resolvido e as parcelas que estão no caixa (só nota lançada). */
public record NotaFiscalListaItemDTO(@JsonUnwrapped NotaFiscalResponseDTO nota, String eventoCodigo, String eventoNome, String proximoVencimento,
        boolean quitada, List<ParcelaLancada> lancamentos) {

    public record ParcelaLancada(Long id, @JsonProperty("parcelaNF") Integer parcelaNF, double valor, String status, String vencimento,
            String dataCompetencia, String dataPagamento, String formaPag) {}
}
