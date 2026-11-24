package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Saldo de um item: físico, reservado, disponível, valor e situação ([tom, rótulo]). */
public record SaldoItemDTO(InsumoResponseDTO insumo, double fisico, double reservado, double disponivel, double valor, List<String> situacao,
        int emFichas, List<Reserva> reservas, double fora) {

    public record Reserva(Long eventoId, String codigo, String nome, double qtd) {}
}
