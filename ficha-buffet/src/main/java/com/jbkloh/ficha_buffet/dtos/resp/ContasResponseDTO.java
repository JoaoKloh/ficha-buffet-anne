package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Contas a pagar ou a receber, com os totais em aberto, vencidos, da semana e pagos em parte. */
public record ContasResponseDTO(String tipo, String filtro, String unidade, Total emAberto, Total vencido, Total seteDias, Parcial parcial,
        List<LancamentoListaItemDTO> contas, int semUnidade) {

    public record Total(double valor, int n) {}

    /** pago: o que já entrou ou saiu nas contas pagas em parte; saldo: o que falta nelas. */
    public record Parcial(double pago, double saldo, int n) {}
}
