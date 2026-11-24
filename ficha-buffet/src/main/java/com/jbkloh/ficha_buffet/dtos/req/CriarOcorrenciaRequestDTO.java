package com.jbkloh.ficha_buffet.dtos.req;

import com.jbkloh.ficha_buffet.calculo.Dominio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Registra o que fugiu do planejado no evento. quem = nome de quem registra; acao = o que foi feito (opcional). */
public record CriarOcorrenciaRequestDTO(
    @NotNull(message = "Informe o evento.") @Positive(message = "Evento inválido.") Long eventoId,
    @NotBlank(message = "Escolha o tipo da ocorrência.") @Pattern(regexp = Dominio.RE_TIPO_OCORRENCIA, message = "Tipo de ocorrência inválido.") String tipo,
    @NotBlank(message = "Informe quem registra.") @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.") String quem,
    @NotBlank(message = "Descreva o que aconteceu.") @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.") String texto,
    @Size(max = 2000, message = "O que foi feito deve ter no máximo 2000 caracteres.") String acao
) {}
