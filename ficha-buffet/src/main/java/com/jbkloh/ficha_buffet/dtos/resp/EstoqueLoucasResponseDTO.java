package com.jbkloh.ficha_buffet.dtos.resp;

import java.util.List;

/** Disponibilidade de louças e utensílios numa data. */
public record EstoqueLoucasResponseDTO(String data, List<EventoNaData> eventosNaData, List<Louca> itens) {

    public record EventoNaData(Long id, String codigo, String nome) {}

    public record Louca(InsumoResponseDTO insumo, double total, double reservado, List<Reserva> reservas, double fora, double disponivel) {}

    public record Reserva(String codigo, double qtd) {}
}
