package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

public record EstoqueSaldoResponseDTO(Kpis kpis, List<SaldoItemDTO> itens, boolean semMovimentos) {

    public record Kpis(double valor, int semPreco, int comSaldo, int consumiveis, int abaixo, int negativos) {}
}
