package com.jbkloh.ficha_buffet.dtos.resp;

/** Diárias que viraram contas a pagar e o vencimento delas (data do evento). */
public record LancarEquipeResponseDTO(int lancados, String vencimento) {}
