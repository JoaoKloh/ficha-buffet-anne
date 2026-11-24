package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Plano de logística do evento; substitui o plano inteiro. Textos nulos viram vazios e horários vazios ficam a definir. */
public record SalvarLogisticaRequestDTO(
    @Size(max = 200, message = "Veículos deve ter no máximo 200 caracteres.")
    String veiculos,
    @Size(max = 120, message = "Motorista deve ter no máximo 120 caracteres.")
    String motorista,
    @Size(max = 120, message = "Viagens deve ter no máximo 120 caracteres.")
    String viagens,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Horário de saída inválido.")
    String saida,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Horário de montagem inválido.")
    String montagem,
    @Pattern(regexp = Dominio.RE_HORA_OU_VAZIA, message = "Horário de retorno inválido.")
    String retorno,
    @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
    String obs
) {}
