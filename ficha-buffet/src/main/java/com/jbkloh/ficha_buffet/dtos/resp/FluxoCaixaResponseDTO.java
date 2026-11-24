package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/**
 * Fluxo de caixa: saldo de cada conta (informado + movimento posterior), o que entrou
 * e saiu no mês e a projeção pelo vencimento do que está em aberto.
 * saldoTotal e os saldos projetados são nulos enquanto nenhum saldo foi informado.
 */
public record FluxoCaixaResponseDTO(String unidade, String mes, List<Conta> contas, boolean temSaldo, double saldoTotal, double recebidoMes, double pagoMes,
        double entradas30, double saidas30, Double projetado30, List<Linha> linhas, String saldosData, int semUnidade) {

    public record Conta(String conta, String rotulo, Double saldo, Double informado, String informadoEm) {}

    /** vencidos = linha dos vencidos em aberto (destacada). */
    public record Linha(String rotulo, double entradas, double saidas, double liquido, Double saldo, boolean vencidos) {}
}
